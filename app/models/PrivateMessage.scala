package models

import java.util.Date
import etc._
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import com.twitter.json.{Json, JsonSerializable}

case class PrivateMessage(id: Long = 0,
                          authorID: Long,
                          receiverID: Long,
                          var title: Option[String] = None,
                          var content: String,
                          writtenAt: Date = new Date,
                          var readAt: Option[Date] = None) extends JsonSerializable {
  def toJson() = {
    val map = Map(
      "id" -> id,
      "author" ->  User.findBy("id" -> authorID.toString).^?.map(_.username).getOrElse("NA"),
      "authorID" -> (if(authorID.?) authorID else -1),
      "title" -> title.getOrElse("No Title"),
      "writtenAt" -> writtenAt,
      "new" -> (if(readAt == None){true}else{false}))
    Json.build(map).toString
  }
}

object PrivateMessage {
  
  def apply(title: String, content: String, from: Long,  to: Long) {
    PrivateMessage(0, from, to, Some(title), content)
  }
  
  def pm = {
    get[Long]("id") ~ get[Long]("authorid") ~ get[Long]("receiverID") ~
    get[Option[String]]("title") ~ get[String]("content") ~ get[Date]("writtenat") ~ 
    get[Option[Date]]("readAt") map  {
      case id~author~receiver~title~content~writtenAt~readAt =>
        PrivateMessage(id, author, receiver, title, content, writtenAt, readAt)
    }
  }
  
  def allWritten(id: Long) = DB.withConnection { implicit c =>
      SQL("select * from PrivateMessage where authorid = {id}").on('id -> id).as(pm *)
  }

  def allReceived(id: Long) = DB.withConnection { implicit c =>
    SQL("select * from PrivateMessage where receiverID = {id}").on('id -> id).as(pm *)
  }

  def findById(id: Long) = DB.withConnection { implicit c =>
    SQL("select * from PrivateMessage where id = {id}").on('id -> id).as(pm *)
  }

  def create(pm: PrivateMessage) {
    pm.content = pm.content.replace("<", "&lt;").replace(">", "&gt;")
    DB.withConnection { implicit c =>
      SQL("""
      insert into PrivateMessage (authorid, receiverid, title, content, writtenat)
      values ({from},{to},{title},{content},{wat})
      """).on('from -> pm.authorID, 'to -> pm.receiverID, 'title -> pm.title,
        'content -> pm.content, 'wat -> pm.writtenAt)
        .executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("delete from PrivateMessage where id = {id}").on('id -> id).executeUpdate()
    }
  }

  def update(pm: PrivateMessage) {
    DB.withConnection { implicit c =>
      SQL("""
          update PrivateMessage
          set readat={readat}
          where id={id}
          """).on('readat -> pm.readAt, 'id -> pm.id).executeUpdate()
    }
  }

  def count(id: Long) = DB.withConnection { implicit c =>
    SQL("select count(*) from PrivateMessage where id = {id}").on('id -> id).as(scalar[Long].single)
  }
}
