/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import actors.ApiKeyManager
import scala.concurrent.Future
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import model.ApiKey
import play.api.mvc.Controller
import db.DB
import play.api.Logger
import org.bson.types.ObjectId
import scala.collection.JavaConversions._



/**
 * The AccessFilter should
 *  - check if the session (timestamp, ip and user in session cookie) is expired and update it 
 *  - check the apikey and find if the call is allowed
 *  
 */
class AccessFilter extends Filter {
  val log = Logger(this.getClass())

  /**
   * Add a new session to the requestHeader, make a new one and return it.
   */
  def withSession( rh:RequestHeader, sessionData: Map[String,String]): RequestHeader = {
          // make a new session cookie
          val newCookie= Seq( Session.encode(sessionData))
          
          // replace cookie in header
          val oldHeaders = rh.headers.toMap
          var newHeaders = oldHeaders - Session.COOKIE_NAME + (( Session.COOKIE_NAME, newCookie))

         rh.copy( headers = new Headers { val data: Seq[(String, Seq[String])] =  (newHeaders.toSeq) } )
  }
  
  
  def effektivUserIds(userId: Option[String], proxyId:Option[String] ): Seq[String] = {
    val result = scala.collection.mutable.ArrayBuffer.empty[String]
    for( id <- userId ) {
        result.add(id)
        val user = DB.getUserDAO.get(new ObjectId( id ))
        val groupIds = user.getUserGroupsIds().map{ x => x.toString() }      
        result.addAll( groupIds )
    }
    for( proxy <- proxyId ) result.add( proxy )
    for( id <- userId ) {
        val user = DB.getUserDAO.get(new ObjectId( id ))
        val groupIds = user.getUserGroupsIds().map{ x => x.toString() }      
        result.addAll( groupIds )
    }
    result.toSeq
  }
  
  def apiKeyCheck(next: (RequestHeader) => Future[Result], rh:RequestHeader):Future[Result] = {
		  implicit val timeout = new Timeout(1000.milliseconds)
		  val access = new ApiKeyManager.Access

		  if( log.isDebugEnabled ) {
			  log.debug( "PATH: " + rh.path )
			  if( ! rh.session.isEmpty ) {
				  val ses = for( entry <- rh.session.data ) yield entry._1+": "+entry._2
						  log.debug( "Session: " + ses.mkString( "", "\n   ", "\n" ))
			  }
		  }

      rh.queryString.get( "apikey") match {
  		  case Some(Seq( key, _ )) => access.apikey = key
  		  case _ => {
  			  rh.session.get( "apikey" ) match {
  			  case Some( key ) => access.apikey = key
  			  case None => access.ip = rh.remoteAddress
  			  }
  		  }
		  } 
      
      val userId = rh.session.get( "user")
		  access.call = rh.path
				  val apiActor = Akka.system.actorSelection("user/apiKeyManager"); 
		  (apiActor ? access).flatMap {
			  response => response match {
          case o:ObjectId => {
               val sessionData = rh.session + (("effektivUserIds", effektivUserIds(userId, Some( o.toString())).mkString(",")))
              val newRh = withSession( rh, sessionData.data )
             next( newRh )            
          }
		  	  case ApiKey.Response.ALLOWED => {
            val sessionData = rh.session + (("effektivUserIds", effektivUserIds(userId, None).mkString(",")))
            val newRh = withSession( rh, sessionData.data )
           next( newRh ) 
          }
		  	  case r:ApiKey.Response => Future.successful( Results.BadRequest( r.toString() ))
		  	  case _ => Future.successful( Results.Forbidden )
			  }
		  }
  }
  
  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
     if( DB.getConf().hasPath( "apikey.ignorePattern")) {
       val pattern = DB.getConf().getString( "apikey.ignorePattern").r.unanchored
       (pattern  findFirstIn rh.path)  match {
         case Some(_) => next(rh)
         case None =>  apiKeyCheck( next, rh )
       }
     } else {
       apiKeyCheck( next, rh )
     }
  }
}
