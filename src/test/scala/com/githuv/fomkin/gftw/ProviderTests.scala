package com.githuv.fomkin.gftw

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets

import utest._
import java.nio.file.{Files, Paths}

import cats.data.Kleisli
import com.github.fomkin.gftw.provider
import fs2.Task
import fs2.Stream
import org.http4s.client.{Client, DisposableResponse}
import org.http4s.{Request, Response}

object ProviderTests extends TestSuite {

  private val googResource = getClass.getResource("/NASDAQ-GOOG.csv")
  private val googPath = Paths.get(googResource.toURI)
  private val googBytes = Files.readAllBytes(googPath)
  private val googData = new String(googBytes, StandardCharsets.UTF_8)

  val tests = Tests {

    "should fetch data from file" - {
      val data = provider
        .financeHistoricalData
        .fetchFromFile(googPath)
        .unsafeRun()
      assert(data == googData)
    }

    "should fetch data from google" - {
      // That client always fetches NASDAQ-GOOG.csv file
      val client = {
        val fakeCancel = Task.now(())
        val service = Kleisli[Task, Request, DisposableResponse] { _ =>
          val dataStream = Stream[Task, Byte](googBytes:_*)
          val response = Response().withBodyStream(dataStream)
          Task.now(DisposableResponse(response, fakeCancel))
        }
        Client(service, fakeCancel)
      }
      val data = provider
        .financeHistoricalData
        .fetchFromGoogle(client, "NASDAQ", "GOOG")
        .unsafeRun()
      assert(data == googData)
    }
  }
}
