package com.github.fomkin.gftw.util

/**
  * @tparam S type of source
  * @tparam R result type
  * @tparam E parsing error type
  */
trait Parser[S, R, E] {
  def parse(source: S): Either[E, R]
}

object Parser {
  def apply[S, R, E](implicit parser: Parser[S, R, E]): Parser[S, R, E] = parser
}
