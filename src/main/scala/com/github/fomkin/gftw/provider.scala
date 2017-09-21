package com.github.fomkin.gftw

import java.nio.file.Path

import com.github.fomkin.gftw.util.BOM._
import fs2.io.file
import fs2.{Chunk, Task}
import org.http4s.client.Client

object provider {

  object financeHistoricalData {

    def fetchFromGoogle(client: Client, market: String, ticker: String): Task[String] = client
      .expect[Chunk[Byte]](s"https://www.google.com/finance/historical?q=$market:$ticker&output=csv")
      .map(bytes => bomedUnicodeBytesToString(bytes.toArray))

    def fetchFromFile(path: Path): Task[String] = file
      .readAll[Task](path, 1024)
      .runLog
      .map(bytes => bomedUnicodeBytesToString(bytes.toArray))
  }

}
