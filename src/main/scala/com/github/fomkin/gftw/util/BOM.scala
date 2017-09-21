package com.github.fomkin.gftw.util

import java.nio.charset.StandardCharsets

object BOM {

  private def bom(xs: Int*) = xs.map(_.toByte)

  private final val bomMapping = List(
    bom(0xEF, 0xBB, 0xBF) -> StandardCharsets.UTF_8,
    bom(0xFE, 0xFF) -> StandardCharsets.UTF_16BE,
    bom(0xFF, 0xFE) -> StandardCharsets.UTF_16LE
  )

  /**
    * Detect bom header and construct correct string
    */
  def bomedUnicodeBytesToString(bytes: Array[Byte]): String = {
    bomMapping.find(tpl => bytes.startsWith(tpl._1)) match {
      case Some((bom, charset)) => new String(bytes.drop(bom.length), charset)
      case None => new String(bytes, StandardCharsets.UTF_8) // BOM not found or not recognized
    }
  }

}
