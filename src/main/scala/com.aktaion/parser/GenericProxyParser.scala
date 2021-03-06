package com.aktaion.parser

import java.sql.Timestamp

import scala.util.{Failure, Success, Try}


object GenericProxyParser extends GenericParser {

  /**
    * Take an input string of the format "12/Dec/2014:13:44:36-0800"
    * and use Java SimpleDateFormat("dd/MMM/yyyy:HH:mm:ssZ") to convert the
    * raw string into a Timestamp type
    *
    * @param s input text
    * @return a value of type [[java.sql.Timestamp]]
    */
  def getTimeStamp(s: String): Timestamp = s match {
    //No timestamp in the field extracted
    case "" => {

      //Takes timestamps of the format "12/Dec/2014:13:44:36-0800"
      //see the oracle line for help on building new formats
      //http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
      val format = new java.text.SimpleDateFormat("dd/MMM/yyyy:HH:mm:ssZ")

      new Timestamp(format.parse("0001-01-01").getTime)
    }
    //Process a string into a timestamp
    case _ => {
      val format = new java.text.SimpleDateFormat("dd/MMM/yyyy:HH:mm:ssZ")

      Try(new Timestamp(format.parse(s).getTime)) match {
        case Success(t) => t
        case Failure(_) => new Timestamp(format.parse("0001-0-01").getTime)
      }
    }
  }


  def tokenizeData(rawInputString: String): Option[GenericProxyLogEvent] = {

    if (rawInputString.size < 5)  { println("Log line too small: " + rawInputString); return None}

    val ris: Array[String] = rawInputString.split(" ")

    //Extract time value data
    val timeString = ris(0).tail //drop the left paren with tail
    val timeOffset = ris(1).dropRight(1) //drop the right paren
    val timeCat = timeString + timeOffset
    val convertedTime: Timestamp = getTimeStamp(timeCat)

    //Key identity/entity information
    val userName = ris(2).tail + " " + ris(3).dropRight(1)
    val sourceIp = ris(4)
    val destinationIp = ris(5)

    //todo determine the name of this field
    val unknownField = ris(6)

    //Split by quote (") to recover user agent
    val quoteSplit = rawInputString.split('"')
    val userAgentString = quoteSplit(11)
    val webReferrerString = quoteSplit(13)

    //Layer 7 http data
    val statusCode = ris(7).toInt
    val cacheResult = ris(8)
    val httpMethod = ris(9).tail
    val urlRequested = ris(10)

    //get a vector of meta data about a class if we have a parsing
    //issue return the null vector of meta data
    val urlMeta = UrlLogic.
      getUrlFromString(urlRequested).getOrElse(UrlDataVector("","","",0,""))

    val httpVersion = ris(11).dropRight(1)
    val domainClassification =  quoteSplit(5)//ris(12).tail + " " + ris(13).dropRight(1)
    val riskClassification = quoteSplit(7)// ris(14) + " " + ris(15)
    val mimeType = quoteSplit(9)
   //  val encodingFormat = ris(17).dropRight(1)

    //Byte related flow info
    //todo check this is correct direction
    val bytesSent = Try(quoteSplit(10).split(" ")(1).toInt).getOrElse(0)
    val bytesReceived = Try(quoteSplit(10).split(" ")(2).toInt).getOrElse(0)

        return Some(
          GenericProxyLogEvent(convertedTime,
            timeString,
            userName,
            sourceIp,
            destinationIp,
            unknownField,
            statusCode,
            cacheResult,
            httpMethod,
            urlRequested,
            httpVersion,
            domainClassification,
            riskClassification,
            mimeType,
       //     encodingFormat,
            bytesSent,
            bytesReceived,
            userAgentString,
            webReferrerString,
            urlMeta
          )
        )
  }
}


