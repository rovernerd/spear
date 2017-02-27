package scraper

import scala.collection.Iterable
import scala.reflect.runtime.universe.WeakTypeTag

import scraper.config.{QueryExecutorClass, Settings}
import scraper.expressions.Expression
import scraper.plans.logical.{LocalRelation, SingleRowRelation}
import scraper.types.{LongType, StructType}

class Context(val queryExecutor: QueryExecutor) {
  def this(settings: Settings) = this(
    Class.forName(settings(QueryExecutorClass)).newInstance() match {
      case q: QueryExecutor => q
    }
  )

  private lazy val values: DataFrame = new DataFrame(SingleRowRelation, this)

  def values(first: Expression, rest: Expression*): DataFrame = values select first +: rest

  def sql(query: String): DataFrame = new DataFrame(queryExecutor.parse(query), this)

  def table(name: Name): DataFrame =
    new DataFrame(queryExecutor.catalog lookupRelation name, this)

  def lift[T <: Product: WeakTypeTag](data: Iterable[T]): DataFrame =
    new DataFrame(LocalRelation(data), this)

  def lift[T <: Product: WeakTypeTag](first: T, rest: T*): DataFrame = lift(first +: rest)

  def range(end: Long): DataFrame = range(0, end)

  def range(begin: Long, end: Long): DataFrame = range(begin, end, 1L)

  def range(begin: Long, end: Long, step: Long): DataFrame = {
    val rows = begin until end by step map { Row apply _ }
    val output = StructType('id -> LongType.!).toAttributes
    new DataFrame(LocalRelation(rows, output), this)
  }
}
