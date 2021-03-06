/*
 * Copyright 2018-2019 ProfunKtor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.profunktor.redis4cats.interpreter.pubsub

import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.algebra.PubSubStats
import dev.profunktor.redis4cats.domain._
import dev.profunktor.redis4cats.streams.Subscription
import dev.profunktor.redis4cats.effect.JRFuture
import fs2.Stream
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection

import dev.profunktor.redis4cats.JavaConversions._

class LivePubSubStats[F[_]: Concurrent: ContextShift, K, V](
    pubConnection: StatefulRedisPubSubConnection[K, V]
) extends PubSubStats[Stream[F, *], K] {

  override def pubSubChannels: Stream[F, List[K]] =
    Stream
      .eval {
        JRFuture(F.delay(pubConnection.async().pubsubChannels()))
      }
      .map(_.asScala.toList)

  override def pubSubSubscriptions(channel: RedisChannel[K]): Stream[F, Subscription[K]] =
    pubSubSubscriptions(List(channel)).map(_.headOption).unNone

  override def pubSubSubscriptions(channels: List[RedisChannel[K]]): Stream[F, List[Subscription[K]]] =
    Stream.eval {
      JRFuture(F.delay(pubConnection.async().pubsubNumsub(channels.map(_.underlying): _*))).flatMap { kv =>
        F.delay(kv.asScala.toList.map { case (k, n) => Subscription(RedisChannel[K](k), n) })
      }
    }

}
