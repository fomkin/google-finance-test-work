import com.github.fomkin.gftw.domain.FinanceHistoricalData
import com.github.fomkin.gftw.domain.FinanceHistoricalData.RowParsingError
import com.github.fomkin.gftw.provider
import com.github.fomkin.gftw.util.{CSV, Parser}
import fs2.{Stream, Task}
import org.http4s.client.blaze._
import org.http4s.client.middleware.FollowRedirect

import scala.language.higherKinds

object Main extends App {

  case class RowParsingErrors(xs: Seq[RowParsingError]) extends Exception

  val httpClient = FollowRedirect(3)(PooledHttp1Client())
  val fhdParser = Parser[CSV, FinanceHistoricalData, Seq[FinanceHistoricalData.RowParsingError]]
  val csvParser = CSV.newStringParser()

  val output = {
    provider.financeHistoricalData.fetchFromGoogle(httpClient, "NASDAQ", "GOOG") map { raw =>
      csvParser.parse(raw) match {
        case Left(error) => error.getMessage
        case Right(csv) =>
          fhdParser.parse(csv) match {
            case Left(errors) => errors.mkString("\n")
            case Right(fhd) => fhd.meanReturn.toString()
          }
      }
    }
  }

  Stream.eval(output)
    .flatMap(s => Stream(s.map(_.toByte):_*))
    .to(fs2.io.stdout[Task])
    .run
    .unsafeRun()
}
