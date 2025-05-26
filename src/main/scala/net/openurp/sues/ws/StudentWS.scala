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

package net.openurp.sues.ws

import org.beangle.commons.bean.Initializing
import org.beangle.commons.json.JsonObject
import org.beangle.commons.logging.Logging
import org.beangle.ems.app.dao.AppDataSourceFactory
import org.beangle.jdbc.query.JdbcExecutor
import org.beangle.webmvc.annotation.{mapping, param, response}
import org.beangle.webmvc.support.ActionSupport

import javax.sql.DataSource

class StudentWS extends ActionSupport, Initializing, Logging {

  var jdbcExecutor: JdbcExecutor = _

  var dataSource: DataSource = _

  override def init(): Unit = {
    val dsf = new AppDataSourceFactory()
    dsf.init()
    dataSource = dsf.result
    jdbcExecutor = new JdbcExecutor(dataSource)
  }

  @response
  @mapping("{code}")
  def index(@param("code") code: String): JsonObject = {
    val data = jdbcExecutor.query("select xh code,xm name,xb gender,to_char(birthday,'yyyy-MM-dd') birthday,zjh idcard,nj grade,xy department,zy major,bj squad,gpa from eams_sues2.v_dth where xh=?", code)
    val jo = new JsonObject
    if (data.nonEmpty) {
      val d = data.head
      jo.add("code", d(0))
      jo.add("name", d(1))
      jo.add("gender", d(2))
      jo.add("birthday", d(3))
      jo.add("idcard", d(4))

      jo.add("grade", d(5))
      jo.add("department", d(6))
      jo.add("major", d(7))
      jo.add("squad", d(8))
      jo.add("gpa", d(9))
    }
    jo
  }
}
