package com.github.fomkin.gftw.domain

import java.time.{LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.github.fomkin.gftw.util.{CSV, Parser}

import scala.util.{Failure, Success, Try}

case class FinanceHistoricalData(items: List[FinanceHistoricalData.Item]) {

  import FinanceHistoricalData.localDateOrdering

  lazy val dailyPrices: List[BigDecimal] = items
    .sortBy(_.date) // older in head, newer in tail
    .map(_.close) // use only close price

  lazy val returns: List[BigDecimal] = {
    dailyPrices.zip(dailyPrices.tail) map {
      case (yesterday, today) =>
        (today - yesterday) / yesterday
    }
  }

  lazy val meanReturn: BigDecimal = {
    val (sum, count) = returns.foldLeft((BigDecimal(0), 0)) {
      case ((s, c), x) => (s + x, c + 1)
    }
    sum / count
  }
}

object FinanceHistoricalData {

  case class Item(date: LocalDate, open: BigDecimal, high: BigDecimal, low: BigDecimal, close: BigDecimal, volume: BigDecimal)
  case class RowParsingError(row: Int, col: Option[Int], source: CSV, message: String)

  final val DateLabel = "Date"
  final val OpenLabel = "Open"
  final val HighLabel = "High"
  final val LowLabel = "Low"
  final val CloseLabel = "Close"
  final val VolumeLabel = "Volume"
  final val ValidLabels = Set(DateLabel, OpenLabel, HighLabel, LowLabel, CloseLabel, VolumeLabel)

  final val ItemDateFormatter = DateTimeFormatter
    .ofPattern("d-MMM-yy")
    .withLocale(Locale.US)

  implicit val csvParser: Parser[CSV, FinanceHistoricalData, Seq[RowParsingError]] = (source: CSV) => {

    def headerIsValid(header: List[String]) = {
      ValidLabels.forall(x => header.contains(x))
    }

    def parseItem(rowIndex: Int, labeledRow: String => (String, Int)) = {

      def parseColumn[T](label: String, f: String => T) = {
        val (value, colIndex) = labeledRow(label)
        Try(f(value)) match {
          case Success(result) => Right(result)
          case Failure(_) => Left(RowParsingError(rowIndex, Some(colIndex), source, "Invalid value format"))
        }
      }

      // Shortcut to parse BigDecimal columns
      val parseColumnBd = parseColumn(_: String, BigDecimal(_))

      for {
        date <- parseColumn(DateLabel, LocalDate.parse(_, ItemDateFormatter))
        open <- parseColumnBd(OpenLabel)
        high <- parseColumnBd(HighLabel)
        low <- parseColumnBd(LowLabel)
        close <- parseColumnBd(CloseLabel)
        volume <- parseColumnBd(VolumeLabel)
      } yield {
        Item(date, open, high, low, close, volume)
      }
    }

    source.rows match {
      case header :: rows if headerIsValid(header) =>
        val (errors, items) = {
          def separate(errors: List[RowParsingError],
            items: List[Item],
            rest: List[Either[RowParsingError, Item]]): (List[RowParsingError], List[Item]) = {
            rest match {
              case Nil => (errors, items)
              case Left(x) :: xs => separate(x :: errors, items, xs)
              case Right(x) :: xs => separate(errors, x :: items, xs)
            }
          }
          separate(Nil, Nil,
            rows.zipWithIndex map {
              case (row, rowIndex) =>
                val labeledRow = header.zip(row.zipWithIndex).toMap
                parseItem(rowIndex + 1, labeledRow)
            }
          )
        }
        if (errors.nonEmpty) Left(errors.reverse)
        else Right(FinanceHistoricalData(items.reverse))
      case Nil => Left(Seq(RowParsingError(0, None, source, s"Header is empty")))
      case _ => Left(Seq(RowParsingError(0, None, source, s"Header should contain $ValidLabels")))
    }
  }

  implicit val localDateOrdering: Ordering[LocalDate] = (a: LocalDate, b: LocalDate) => {
    implicitly[Ordering[Long]].compare(
      a.atStartOfDay(ZoneOffset.UTC).toEpochSecond,
      b.atStartOfDay(ZoneOffset.UTC).toEpochSecond
    )
  }
}
