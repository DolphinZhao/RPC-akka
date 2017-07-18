package com.qihoo.rpc

import akka.actor.{Props, ActorSystem, ActorSelection, Actor}
import com.typesafe.config.ConfigFactory

/**
 * Created by zhaozhuohui on 2017/7/18.
 */
class Worker(val masterHost:String, val masterPort:Int) extends Actor{

  var master:ActorSelection = _

  override def preStart(): Unit ={
    master = context.actorSelection("akka.tcp://MasterSystem@127.0.0.1:8888/user/Master")
    master ! "connect"
  }

  override def receive:Receive = {
    case "reply" => {
      println("a reply from master")
    }
  }

}

object Worker {

  def main(args: Array[String]) {
    val host = args(0)
    val port = args(1).toInt
    val masterHost = args(2)
    val masterPort = args(3).toInt
    // 准备配置
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    //ActorSystem老大，辅助创建和监控下面的Actor，他是单例的
    val actorSystem = ActorSystem("WorkerSystem", config)
    actorSystem.actorOf(Props(new Worker(masterHost, masterPort)), "Worker")
    actorSystem.awaitTermination()
  }

}
