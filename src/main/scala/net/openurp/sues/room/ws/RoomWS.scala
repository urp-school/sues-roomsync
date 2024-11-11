/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openurp.sues.room.ws

import com.google.gson.Gson
import org.beangle.commons.bean.Initializing
import org.beangle.commons.codec.digest.Digests
import org.beangle.commons.collection.Collections
import org.beangle.commons.json.JsonQuery
import org.beangle.commons.lang.Strings
import org.beangle.commons.lang.time.{WeekDay, WeekState}
import org.beangle.commons.logging.Logging
import org.beangle.web.action.annotation.{mapping, param, response}
import org.beangle.web.action.support.ActionSupport

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate, LocalDateTime}
import java.util as ju
import scala.collection.mutable

class RoomWS extends ActionSupport, Initializing, Logging {
  var appCode: String = _
  var secret: String = _
  private val formater = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
  private var client: HttpClient = _
  private val gson = new Gson()

  override def init(): Unit = {
    val b = HttpClient.newBuilder()
    b.connectTimeout(Duration.ofSeconds(10))
    client = b.build()
  }

  @response
  @mapping("/free/{schoolYear}/{semesterName}")
  def index(@param("schoolYear") schoolYear: String, @param("semesterName") semesterName: String): String = {
    val weekNum = getLong("weeks").getOrElse(0L)
    if weekNum == 0 then return "缺少参数:weeks,类似weeks=1023"
    val weeks = new WeekState(weekNum).weeks
    val units = get("units", "")
    if Strings.isBlank(units) then return "缺少参数:units,类似units=3-4;3-5"
    val minCapacity = getInt("minCapacity", 1)

    val unitPairs = convertUnits(units)
    val freeRooms = Collections.newBuffer[Set[String]]
    unitPairs foreach { unitPair =>
      val r = getFreeRooms(schoolYear, semesterName, weeks, unitPair._1, unitPair._2._1, unitPair._2._2, minCapacity)
      freeRooms.addOne(r)
    }
    if (freeRooms.size == 1) {
      logger.debug(freeRooms.head.mkString(","))
      freeRooms.head.mkString(",")
    } else {
      var first = freeRooms.head
      for (i <- 1 until freeRooms.size) {
        val second = freeRooms(i)
        first = first.intersect(second)
      }
      logger.debug(first.mkString(","))
      first.mkString(",")
    }
  }

  @response
  @mapping("/remove/{schoolYear}/{semesterName}")
  def remove(): String = {
    val weekNum = getLong("weeks").getOrElse(0L)
    if weekNum == 0 then return "缺少参数:weeks,类似weeks=1023"
    val weeks = new WeekState(weekNum).weeks
    val units = get("units", "")
    if Strings.isBlank(units) then return "缺少参数:units,类似units=3-4;3-5"
    val roomCode = get("roomCode", "")
    if Strings.isBlank(roomCode) then return "缺少参数:roomCode,类似roomCode=D301"
    val beginOn = getDate("beginOn").get

    val unitPairs = convertUnits(units)
    val messages = Collections.newBuffer[String]
    unitPairs foreach { unitPair =>
      val dates = toDates(beginOn, WeekDay.of(unitPair._1), weeks)
      val message = removeOccupy(dates, unitPair._2, roomCode)
      messages.addOne(message)
    }
    messages.mkString(",")
  }

  @response
  @mapping("/new/{schoolYear}/{semesterName}")
  def add(): String = {
    val weekNum = getLong("weeks").getOrElse(0L)
    if weekNum == 0 then return "缺少参数:weeks,类似weeks=1023"
    val weeks = new WeekState(weekNum).weeks
    val units = get("units", "")
    if Strings.isBlank(units) then return "缺少参数:units,类似units=3-4;3-5"
    val roomCode = get("roomCode", "")
    if Strings.isBlank(roomCode) then return "缺少参数:roomCode,类似roomCode=D301"
    val beginOn = getDate("beginOn").get

    val unitPairs = convertUnits(units)
    val messages = Collections.newBuffer[String]
    unitPairs foreach { unitPair =>
      val dates = toDates(beginOn, WeekDay.of(unitPair._1), weeks)
      val message = newOccupy(dates, unitPair._2, roomCode)
      messages.addOne(message)
    }
    messages.mkString(",")
  }

  private def newOccupy(dates: List[LocalDate], units: (Int, Int), roomCode: String): String = {
    val unitsStr = (units._1 to units._2).mkString(",")
    val segments = dates.map { date =>
      s"""{"mod":"add","code":"${roomCode}","units":[${unitsStr}],"startDate":"${date}","endDate":"${date}"}"""
    }.mkString(",")
    val body = s"""{"dateSegment":[${segments}]}"""

    val timestamp = formater.format(LocalDateTime.now)
    val sign = s"appCode=${appCode}&timestamp=${timestamp}&secret=${secret}"
    val signature = Digests.md5Hex(sign).toLowerCase

    val r = HttpRequest.newBuilder()
    r.uri(URI.create("https://jxfw.sues.edu.cn/manager/openapi/room/add-data"))
    r.header("timestamp", timestamp)
    r.header("sign", signature)
    r.header("appCode", appCode)
    r.header("accept", "application/json")
    r.header("content-type", "application/json")
    r.timeout(Duration.ofSeconds(10))
    r.POST(HttpRequest.BodyPublishers.ofString(body))

    logger.info(body)
    val res = client.send(r.build(), HttpResponse.BodyHandlers.ofString()).body()
    logger.info(res)
    val json = gson.fromJson(res, classOf[ju.Map[String, Object]])
    JsonQuery.get(json, "data").toString
  }

  private def removeOccupy(dates: List[LocalDate], units: (Int, Int), roomCode: String): String = {
    val unitsStr = (units._1 to units._2).mkString(",")
    val segments = dates.map { date =>
      s"""{"mod":"remove","code":"${roomCode}","units":[${unitsStr}],"startDate":"${date}","endDate":"${date}"}"""
    }.mkString(",")
    val body = s"""{"dateSegment":[${segments}]}"""
    val timestamp = formater.format(LocalDateTime.now)
    val sign = s"appCode=${appCode}&timestamp=${timestamp}&secret=${secret}"
    val signature = Digests.md5Hex(sign).toLowerCase

    val r = HttpRequest.newBuilder()
    r.uri(URI.create("https://jxfw.sues.edu.cn/manager/openapi/room/add-data"))
    r.header("timestamp", timestamp)
    r.header("sign", signature)
    r.header("appCode", appCode)
    r.header("accept", "application/json")
    r.header("content-type", "application/json")
    r.timeout(Duration.ofSeconds(10))
    r.POST(HttpRequest.BodyPublishers.ofString(body))
    logger.info(body)
    val res = client.send(r.build(), HttpResponse.BodyHandlers.ofString()).body()
    logger.info(res)
    val json = gson.fromJson(res, classOf[ju.Map[String, Object]])
    JsonQuery.get(json, "msg").toString
  }

  private def getFreeRooms(schoolYear: String, semesterName: String, weeks: List[Int],
                           weekday: Int, startUnit: Int, endUnit: Int, minCapacity: Int): Set[String] = {
    val body =
      s"""
         |{
         |"year":"${schoolYear}","semester":"${semesterName}","weeks":"${weeks.mkString(",")}","week":"${weekday}","startUnit":"${startUnit}",
         |"endUnit":"${endUnit}","building":""
         |}
         |""".stripMargin.trim()
    val timestamp = formater.format(LocalDateTime.now)
    val sign = s"appCode=${appCode}&timestamp=${timestamp}&secret=${secret}"
    val signature = Digests.md5Hex(sign).toLowerCase

    val r = HttpRequest.newBuilder()
    r.uri(URI.create("https://jxfw.sues.edu.cn/manager/openapi/room/get-room"))
    r.header("timestamp", timestamp)
    r.header("sign", signature)
    r.header("appCode", appCode)
    r.header("accept", "application/json")
    r.header("content-type", "application/json")
    r.timeout(Duration.ofSeconds(10))
    logger.debug(body)
    r.POST(HttpRequest.BodyPublishers.ofString(body))

    //val res = sample
    val res = client.send(r.build(), HttpResponse.BodyHandlers.ofString()).body()
    val json = gson.fromJson(res, classOf[ju.Map[String, Object]])
    if (JsonQuery.get(json, "code").toString == "200") {
      val rooms = JsonQuery.get(json, "data").asInstanceOf[ju.ArrayList[ju.Map[String, Any]]]
      import scala.jdk.javaapi.CollectionConverters.asScala
      asScala(rooms).filter(room => room.get("seats").toString.toInt >= minCapacity).map(room => room.get("nameZh").asInstanceOf[String]).toSet
    } else {
      Set.empty
    }
  }

  private def toDates(beginOn: LocalDate, weekDay: WeekDay, weeks: List[Int]): List[LocalDate] = {
    val dates = Collections.newBuffer[LocalDate]
    val firstWeekday = beginOn.getDayOfWeek.getValue
    var timeBeginOn = beginOn
    while (weekDay.id != timeBeginOn.getDayOfWeek.getValue) timeBeginOn = timeBeginOn.plusDays(1)
    weeks foreach { w =>
      dates.addOne(timeBeginOn.plusDays((w - 1) * 7))
    }
    dates.toList
  }

  /**
   * 4-5;4-8
   *
   * @param units
   * @return
   */
  private def convertUnits(units: String): Map[Int, (Int, Int)] = {
    val pairs = Strings.split(units, ',')
    val results = new mutable.HashMap[Int, (Int, Int)]
    pairs foreach { result =>
      val weekday = Strings.substringBefore(result, "-").toInt
      val unit = Strings.substringAfter(result, "-").toInt
      results.get(weekday) match {
        case Some(pair) =>
          val (start, end) = pair
          results.put(weekday, (Math.min(start, unit), Math.max(end, unit)))
        case None =>
          results.put(weekday, (unit, unit))
      }
    }
    results.toMap
  }
}
