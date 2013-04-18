package com.quantifind.sumac

import scala.annotation.tailrec
import java.lang.reflect.{Type, Field}
import collection.mutable.LinkedHashMap
import collection._

class ArgumentParser[T <: ArgAssignable] (val argHolders: Seq[T]) {
  lazy val nameToHolder = (LinkedHashMap.empty ++ argHolders.map(a => a.getName -> a)).withDefault { arg =>
    throw new ArgException("unknown option %s\n%s".format(arg, helpMessage))
  }

  def parse(args: Array[String]): Map[T, ValueHolder[_]] = {
    parse(ArgumentParser.argListToKvMap(args))
  }

  def parse(rawKvs: Map[String,String]): Map[T, ValueHolder[_]] = {
    rawKvs.map{case(argName, argValue) =>
      val holder = nameToHolder(argName)
      val result = try {
        ParseHelper.parseInto(argValue, holder.getType, holder.getCurrentValue) getOrElse {
          throw new ArgException("don't know how to parse type: " + holder.getType)
        }
      } catch {
        case ae: ArgException => throw ae
        case e: Throwable => throw new ArgException("Error parsing \"%s\" into field \"%s\" (type = %s)\n%s".format(argValue, argName, holder.getType, helpMessage))
      }
      holder -> result
    }
  }

  def helpMessage: String = {
    val msg = new StringBuilder
    msg.append("usage: \n")
    nameToHolder.foreach { case (k, v) =>
      msg.append("--%s\t%s\t%s\n\n".format(k, v.getType, v.getDescription))
    }
    msg.toString
  }
}

object ArgumentParser {
  def apply[T <: ArgAssignable](argHolders: Traversable[T]) = {
    // ignore things we don't know how to parse
    new ArgumentParser(argHolders.toSeq.filter(t => ParseHelper.findParser(t.getType).isDefined))
  }

  def argListToKvMap(args: Array[String]): Map[String,String] = {
    @tailrec
    def parse(args: List[String], acc: mutable.Map[String, String] = mutable.Map.empty): mutable.Map[String,String] = {
      args match {
        case Nil => acc
        case "--help" :: _ =>
          acc("help") = null
          acc
        case arg :: _ if (!arg.startsWith("--")) =>
          throw new ArgException("expecting argument name beginning with \"--\", instead got %s".format(arg))
        case name :: value :: tail =>
          val suffix = name.drop(2)
          acc(suffix) = value
          parse(tail, acc)
        case _ => throw new ArgException("gave a non-key value argument")
      }
    }
    parse(args.toList)
  }
}

/**
 * Container for one argument, that has name, type, and can be assigned a value.
 */
trait ArgAssignable {
  def getName: String
  def getDescription: String
  def getType: Type
  def getCurrentValue: AnyRef
  def setValue(value: Any)
}

class FieldArgAssignable(val field: Field, val obj: Object) extends ArgAssignable {
  field.setAccessible(true)
  val annotationOpt = Option(field.getAnnotation(classOf[Arg]))

  def getName = {
    val n = annotationOpt.map(_.name).getOrElse(field.getName)
    if (n == "") field.getName else n
  }

  def getDescription = {
    val d = annotationOpt.map(_.description).getOrElse(field.getName)
    if (d == "") getName else d
  }

  def getType = field.getGenericType
  def getCurrentValue = field.get(obj)

  def setValue(value: Any) = {
    field.set(obj, value)
  }
}

case class ArgException(msg: String, cause: Throwable) extends IllegalArgumentException(msg, cause) {
  def this(msg: String) = this(msg, null)
}
