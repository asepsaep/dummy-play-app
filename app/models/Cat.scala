package models

object CatTypes extends Enumeration {
  type CatType = Value
  val Persia, Anggora, Garong = Value
}

import CatTypes._
case class Cat(id: Long, name: List[String], types: CatType)