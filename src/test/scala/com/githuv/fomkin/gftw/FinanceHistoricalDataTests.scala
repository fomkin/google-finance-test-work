package com.githuv.fomkin.gftw

import java.time.{LocalDate, Month}

import com.github.fomkin.gftw.domain.FinanceHistoricalData
import com.github.fomkin.gftw.util.{CSV, Parser}
import utest._

object FinanceHistoricalDataTests  extends TestSuite {

  private final val E = BigDecimal(0.01)

  private val csv = CSV(
    List(
      List("Date", "Open", "High", "Low", "Close", "Volume"),
      List("21-Sep-17","0","0","0","10","0"),
      List("20-Sep-17","0","0","0","5","0"),
      List("19-Sep-17","0","0","0","3","0")
    )
  )

  private val fhd = FinanceHistoricalData(
    List(
      FinanceHistoricalData.Item(LocalDate.of(2017, Month.SEPTEMBER, 21), 0, 0, 0, 10, 0),
      FinanceHistoricalData.Item(LocalDate.of(2017, Month.SEPTEMBER, 20), 0, 0, 0, 5, 0),
      FinanceHistoricalData.Item(LocalDate.of(2017, Month.SEPTEMBER, 19), 0, 0, 0, 3, 0)
    )
  )

  private val csvWithBrokenValues = CSV(
    List(
      List("Date", "Open", "High", "Low", "Close", "Volume"),
      List("21-Sep-17","0","0","meow","10","0"),
      List("20-Sep-17","0","cow","0","5","0")
    )
  )

  private val csvWithBrokenHeader = CSV(
    List(
      List("Date", "Open", "Meow", "Low", "Close", "Volume"),
      List("21-Sep-17","0","0","0","10","0"),
      List("20-Sep-17","0","0","0","5","0")
    )
  )

  val tests = Tests {

    val fhdParser = Parser[CSV, FinanceHistoricalData, Seq[FinanceHistoricalData.RowParsingError]]

    "parser should be parsed from csv" - {
      val result = fhdParser.parse(csv)
      val pattern = Right(fhd)
      assert(result == pattern)
    }

    "parser should detect malformed header" - {
      val result = fhdParser.parse(csvWithBrokenHeader)
      val pattern = Left(
        Seq(FinanceHistoricalData.RowParsingError(0, None, csvWithBrokenHeader,
          "Header should contain Set(Volume, High, Close, Open, Date, Low)"))
      )
      assert(result == pattern)
    }

    "parser should detect malformed values of columns" - {
      val result = fhdParser.parse(csvWithBrokenValues)
      val pattern = Left(
        Seq(
          FinanceHistoricalData.RowParsingError(1, Some(3), csvWithBrokenValues, "Invalid value format"),
          FinanceHistoricalData.RowParsingError(2, Some(2), csvWithBrokenValues, "Invalid value format"),
        )
      )
      assert(result == pattern)
    }

    "should collect daily prices" - {
      val result = fhd.dailyPrices
      val pattern = List[BigDecimal](3, 5, 10)
      assert(result == pattern)
    }

    "should collect daily returns" - {
      val result = fhd.returns
      val a = BigDecimal(0.66)
      val b = BigDecimal(1)
      assert(result.length == 2)
      assert((result(0) - a).abs < E)
      assert((result(1) - b).abs < E)
    }

    "should calculate mean return" - {
      val result = fhd.meanReturn
      val pattern = BigDecimal(0.83)
      assert((result - pattern).abs < E)
    }
  }

}
