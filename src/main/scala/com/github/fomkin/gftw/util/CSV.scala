package com.github.fomkin.gftw.util

case class CSV(rows: List[List[String]])

object CSV {

  def newStringParser(delimiter: Char = ','): Parser[String, CSV, Throwable] = (source: String) => {
    try {
      // Very naive. Don't cate about corner cases
      val rowSources = source.split('\n').toList
      val csv = CSV(rowSources.map(_.split(delimiter).toList))
      Right(csv)
    }
    catch {
      case cause: Throwable =>
        Left(cause)
    }
  }
}
