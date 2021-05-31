package schema

import scala.util.parsing.input.Positional

/// Syntactic Domain
package syntax {
  enum Constant {
    case Bool(isTrue: Boolean)
    case Num(value: BigDecimal)
    case Char(value: scala.Char)
    case String(value: scala.Predef.String)
  }

  type Identifier = String

  enum Expression extends Positional {
    case Const(value: Constant)
    case Var(id: Identifier)
    case ProcedureCall(operator: Expression, operands: List[Expression])
    case LambdaExpr(
        formals: List[Identifier],
        commands: List[Command],
        returnValue: Expression
    )
    case Conditional(
        test: Expression,
        consequent: Expression,
        alternate: Expression
    )
    case Conditional_NoAlt(test: Expression, consequent: Expression)
    case Assignment(id: Identifier, value: Expression)
    // A simplified definition AST
    case Definition(id: Identifier, value: Expression)
  }

  type Command = Expression
}

// Semantic domains
package semantics {
  // the meaning of an expression under some environment
  type Computation = ExpressedValue
  // the procedure type
  type Procedure = List[ExpressedValue] => Computation

  enum ExpressedValue {
    case Symbol(symbol: scala.Predef.String)
    case Char(c: Character)
    case Num(num: BigDecimal)
    case String(s: scala.Predef.String)
    case Pair(first: ExpressedValue, second: ExpressedValue)
    case Bool(isTrue: Boolean)
    case Procedure(f: semantics.Procedure)
  }

  type Nameable = ExpressedValue

  // This kind of functional definition is inspired from DCPL: Design concepts in
  // Programming Languages, page 277
  // 
  // We can also define Env as a class, which stores mappings of (ID, Value)
  //
  // But in this project `Environment` would not be used directly; instead a mutable version
  // called `MutableEnvironment` wraps this immutable one and provides ways to modify itself.
  // Using mutable version is because REPL changes environment by nature, e.g. `(define ...)`
  type Environment = (syntax.Identifier) => Option[Nameable]

  object Environment {
    val emptyEnvironment: Environment = id => None
  }

  extension (env: Environment) {
    def lookup(id: syntax.Identifier): Option[Nameable] = env(id)

    // return a new environment with new bindings added
    def extend(bindings: Map[syntax.Identifier, Nameable]): Environment = {
      bindings match {
        case b if b.isEmpty => env
        case b => id => if b.contains(id) then Some(b(id)) else env(id)
      }
    }
  }

  /// A wrapper of `Environment` which is mutable, to support things like `define` in REPL
  class MutableEnvironment(var env: Environment) {
    // still immutable methods
    // either use (id) => ... or `def`
    // DON'T write `val lookup = env.lookup`, since `env` will change and this would
    // make this `lookup` value fixed, i.e. the env would directly be evaluated
    val lookup = (id) => env.lookup(id)
    val extend = (b: Map[syntax.Identifier, Nameable]) => MutableEnvironment(env.extend(b))

    // A mutable method
    def addEntry(id: syntax.Identifier, value: Nameable) = {
      val oldEnv = env
      // make sure to use oldEnv, or infinite recursion would happen
      this.env = ID => if ID == id then Some(value) else oldEnv(ID)
    }
  }

  object MutableEnvironment {
    val emptyEnvironment = MutableEnvironment(id => None)
  }
}
