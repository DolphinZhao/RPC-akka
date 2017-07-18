package com.qihoo.rpc

/**
 * Created by zhaozhuohui on 2017/7/18.
 */
trait RemoteMessage extends Serializable{

}

//worker发送到Master
case class RegisterWorker(id:String, momory:Int, cores:Int) extends RemoteMessage

//Master到worker的消息
case class RegisteredWorker(masterUrl:String)

//worker -> worker
case object SendHeartbeat

case class Heartbeat(id:String) extends RemoteMessage

//Master -> self
case object CheckTimeOutWorker