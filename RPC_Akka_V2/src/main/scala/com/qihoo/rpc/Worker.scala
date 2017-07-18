package com.qihoo.rpc

import java.util.UUID

import akka.actor.{Props, ActorSystem, ActorSelection, Actor}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/**
 * Created by zhaozhuohui on 2017/7/18.
 */
class Worker(val masterHost:String, val masterPort:Int, val memory:Int, val cores:Int) extends Actor{

  //master的引用
  var master:ActorSelection = _
  val workerId = UUID.randomUUID().toString //workerId
  val CHECK_INTERVAL = 10000  //间隔时间

  override def preStart(): Unit ={
    //和master建立连接
    master = context.actorSelection("akka.tcp://MasterSystem@127.0.0.1:8888/user/Master")
    //向Master发送注册消息
    master ! RegisterWorker(workerId, memory, cores)
  }

  override def receive:Receive = {
    case RegisteredWorker(masterUrl) => {
      println(masterUrl)
      //启动定时器发送心跳,导入隐式转换
      import context.dispatcher
      context.system.scheduler.schedule(0 millis, CHECK_INTERVAL millis, self, SendHeartbeat)
    }
      //发送心跳
    case SendHeartbeat => {
      //向master发送心跳，此处还可以写一些业务逻辑
      println("send heartbeat to master")
      master ! Heartbeat(workerId)
    }
  }

}

object Worker {

  def main(args: Array[String]) {
    val host = args(0)
    val port = args(1).toInt
    val masterHost = args(2)  //master的IP
    val masterPort = args(3).toInt  //master的port
    val memory = args(4).toInt  //存储资源信息
    val cores = args(5).toInt //cpu计算资源信息
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
    actorSystem.actorOf(Props(new Worker(masterHost, masterPort, memory, cores)), "Worker")
    actorSystem.awaitTermination()
  }

}
