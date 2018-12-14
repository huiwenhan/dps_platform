/*
 *   HORTONWORKS DATAPLANE SERVICE AND ITS CONSTITUENT SERVICES
 *
 *   (c) 2016-2018 Hortonworks, Inc. All rights reserved.
 *
 *   This code is provided to you pursuant to your written agreement with Hortonworks, which may be the terms of the
 *   Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *   to distribute this code.  If you do not have a written agreement with Hortonworks or with an authorized and
 *   properly licensed third party, you do not have any rights to this code.
 *
 *   If this code is provided to you under the terms of the AGPLv3:
 *   (A) HORTONWORKS PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) HORTONWORKS DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *     LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) HORTONWORKS IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *     FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, HORTONWORKS IS NOT LIABLE FOR ANY
 *     DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED TO,
 *     DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY,
 *     OR LOSS OR CORRUPTION OF DATA.
 */

package com.hortonworks.dataplane.cs.sync

import akka.actor.ActorRef
import com.hortonworks.dataplane.commons.domain.Entities
import com.hortonworks.dataplane.commons.domain.Entities.{ClusterService => ClusterServiceData}
import com.hortonworks.dataplane.cs.{CredentialInterface, StorageInterface}
import com.hortonworks.dataplane.cs.sync.TaskStatus.TaskStatus
import com.hortonworks.dataplane.cs.sync.TaskType.TaskType
import com.hortonworks.dataplane.cs.tls.SslContextManager
import com.typesafe.config.Config
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FetchHdfsTask(cl: ClusterData, c: Config, w: WSClient, si: StorageInterface,
                    credentialInterface: CredentialInterface, cs: ActorRef) extends ClusterSyncTask(cl,c,w,si, credentialInterface, cs) {

  override val taskType: TaskType = TaskType.Hdfs

  override def executeTask(implicit hJwtToken: Option[Entities.HJwtToken]): Future[TaskStatus] = {

   ambariInterface.getHdfsInfo(hJwtToken).flatMap { hdfs =>
     if (hdfs.isRight) {
       val hdfsInfo = hdfs.right.get
       val props = Try(hdfsInfo.props).getOrElse(None)
       val toPersist = ClusterServiceData(serviceName = "HDFS",
         properties = props,
         clusterId = Some(cl.cluster.id.get))

       upsertService(cl.cluster, toPersist, Nil)
         .map(_ => TaskStatus.Complete)

     } else {
       log.error(
         s"Error saving HDFS info, HDFS data was not returned",
         hdfs.left.get)
       Future.successful(TaskStatus.Failed)
     }
   }

  }
}
