package com.githuv.fomkin.gftw

import com.github.fomkin.gftw.util.CSV
import utest._

object CSVParserTests extends TestSuite {

  val tests = Tests {

    "should parse csv" - {
      val parser = CSV.newStringParser()
      val csv = parser.parse("1,2,3\n4,5,6")
      assert(csv == Right(CSV(List(List("1","2","3"), List("4","5","6")))))
    }
  }
}
