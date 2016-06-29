/*
 * Copyright 2015-2016 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package whisk.core.entity

import scala.util.Try

import spray.json.JsNumber
import spray.json.JsValue
import spray.json.RootJsonFormat
import spray.json.deserializationError
import scala.util.Success
import scala.util.Failure

/**
 * LogLimit encapsulates allowed amount of logs written by an action.
 *
 * It is a value type (hence == is .equals, immutable and cannot be assigned null).
 * The constructor is private so that argument requirements are checked and normalized
 * before creating a new instance.
 *
 * FIXME: Int because of JSON deserializer vs. <code>ByteSize</code> and compatibility
 * with <code>MemoryLimit</code>
 *
 * @param megabytes the memory limit in megabytes for the action
 */
protected[core] class LogLimit private (val megabytes: Int) extends AnyVal {
    protected[core] def apply() = megabytes
}

protected[core] object LogLimit extends ArgNormalizer[LogLimit] {
    protected[core] val STD_LOGSIZE = 10 // MB

    /** Gets LogLimit with default log limit */
    protected[core] def apply(): LogLimit = new LogLimit(STD_LOGSIZE)

    /**
     * Creates LogLimit for limit. Only the default limit is allowed currently.
     *
     * @param megabytes the limit in megabytes, must be within permissible range
     * @return LogLimit with limit set
     * @throws IllegalArgumentException if limit does not conform to requirements
     */
    @throws[IllegalArgumentException]
    private def apply(megabytes: Int): LogLimit = {
        require(megabytes == STD_LOGSIZE, s"only standard log limit of '$STD_LOGSIZE' (megabytes) allowed")
        new LogLimit(megabytes);
    }

    override protected[core] implicit val serdes = new RootJsonFormat[LogLimit] {
        def write(m: LogLimit) = JsNumber(m.megabytes)

        def read(value: JsValue) = Try {
            val JsNumber(mb) = value
            require(mb.isWhole(), "log limit must be whole number")
            LogLimit(mb.intValue)
        } match {
            case Success(limit)                       => limit
            case Failure(e: IllegalArgumentException) => deserializationError(e.getMessage, e)
            case Failure(e: Throwable)                => deserializationError("log limit malformed", e)
        }
    }
}
