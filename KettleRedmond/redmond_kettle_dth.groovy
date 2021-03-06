
/**
 *  Kettle_temp
 *
 *  Copyright 2020 Boris Emchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * 1) Capability дает стандартный набор аттрибутов? Какой? Мне нужно: switch, energy потреблено, кол-во запусков, время работы)
 *    1.1) Все остальные задавать как кастомные? Или можно ничего не делать?
 * 2) Какие стандартные METHODS возможны? Какие обязательны? В какой ситуации возникают?
 *	2.1) что уже знаю:
 * 	- Parse вызывается, когда отрабатывает runCmd уже сразу с текстом ответа
 *  - Updated вызвается, когда нажимаешь "сохранить" в конфигурации
 *  - Installed точно когда-то вызывается - но пока не ясно в каких случаях:)
 * 3) Как работает sendEvent?
 */
metadata {
	definition (name: "KettleRedmondDTH", namespace: "BorisE", author: "Boris Emchenko") {
		capability "Switch"
        capability "Temperature Measurement"
        capability "Power Meter"
        capability "Refresh"
	
        attribute "version", "string"
	}

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
		input name: "about", type: "paragraph", element: "paragraph", title: "Redmond Kettler 0.2a", description: "By Boris Emchenko"
    }
    
	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) 
	{
 		standardTile("switch", "device.switch", width: 6, height: 4, decoration: "flat", canChangeIcon: true) {
			state "off", label:'${name}', action:"switch.on", icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#ffffff"
    		state "on", label:'${name}', action:"switch.off", icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#00a0dc"
			state "turningOn", label:'Turning on', icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#00a0dc", nextState: "turningOff"
			state "turningOff", label:'Turning off', icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#ffffff", nextState: "turningOn"
            }
    	
        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
			state "temperature", label:'${currentValue}°',
			backgroundColors:[
				[value: 31, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			]
		}
		valueTile("mode", "device.mode", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue}'
		}

       
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"refresh.refresh", icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/refresh-icon%402x.png"
		}

		valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} kWh'
		}
		valueTile("duration", "device.duration", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} min'
		}
		valueTile("times", "device.times", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue}'
		}

  		// the "switch" tile will appear in the Things view
        main("switch")
        
        details(["switch", "temperature", "mode", "refresh", "energy", "duration", "times"])
	}
}

def installed() {
	log.debug "installing..."

	//set defaule values
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "temperature", value: 0)
	sendEvent(name: "mode", value: "00")
    sendEvent(name: "energy", value: 0)
    sendEvent(name: "duration", value: 0)
    sendEvent(name: "times", value: 0)
}


def on() {
	log.debug "on()"
    //sendEvent(name: "temperature", value: 95)
    //sendEvent(name: "energy", value: 2001)
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug "off()"
    //sendEvent(name: "temperature", value: 31)
    //sendEvent(name: "energy", value: 1001)
	sendEvent(name: "switch", value: "off")
}

def refresh() {
	log.debug "Refresh method call"
	
    //installed()
    updated()
    //get_data()

	/*
	// Only allow refresh every 2 minutes to prevent flooding the Zwave network
	def timeNow = now()
	if (!state.refreshTriggeredAt || (2 * 60 * 1000 < (timeNow - state.refreshTriggeredAt))) {
		state.refreshTriggeredAt = timeNow
		// refresh will request battery, prevent multiple request by setting lastbatt now
		state.lastbatt = timeNow
		// use runIn with overwrite to prevent multiple DTH instances run before state.refreshTriggeredAt has been saved
		runIn(2, "pollDevice", [overwrite: true])
	}
    */
}

def updated(){
	log.debug "Updated method call"

	log.info("Hub address: " + device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP"))
    log.info("Device IP ${DeviceIP}:${DevicePort}")

	runCmd("update")
}


def runCmd(String varCommand) {
	
    log.info("Running command [${varCommand}]")

	def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"
	log.debug "The device id configured is: $device.deviceNetworkId"
	
	//def userpassascii = "${HTTPUser}:${HTTPPassword}"
	//def userpass = "Basic " + userpassascii.encodeAsBase64().toString()

	//def path = DevicePath
	def path = "/index.php?cmd=4"
	log.debug "path is: $path"
	def body = ""//varCommand
	log.debug "body is: $body"

	def headers = [:]
	headers.put("HOST", "$host:$DevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	log.debug "The Header is $headers"
	def method = "GET"

	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		hubAction.options = [outputMsgToS3:false]
		//log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}


private def parseEventMessage(String description) {
    def event = [:]
    def parts = description.split(',')
    parts.each { part ->
        part = part.trim()
        
        if (part.startsWith('headers')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                event.headers = valueString
            }
        }
        else if (part.startsWith('body')) {
            part -= "body:"
            def valueString = part.trim()
            if (valueString) {
                event.body = valueString
            }
        }
    }

    event
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing message: '${description}'"
    
    def parsedEvent= parseEventMessage( description)
	log.debug "AfterParsingEventMessage '${parsedEvent}'"

    def headerString = new String(parsedEvent.headers.decodeBase64())
    log.debug "Response header '${headerString}'"
    
    def bodyString = new String(parsedEvent.body.decodeBase64())
    log.debug "Response body '${bodyString}'"
    
    def json = new groovy.json.JsonSlurper().parseText( bodyString)
	log.trace json //{alltime=11.1, durat=80, mode=00, status=00, targettemp=100, temp=25, times=279, watts=24528}
    
    
    if (json.status)
    {
	    if (json.mode == "00") {
			sendEvent(name: "switch", value: "off")
		}elseif(json.mode == "02")	{
			sendEvent(name: "switch", value: "on")
		}else{
			sendEvent(name: "switch", value: "turningOn")
		}
   	}
    if (json.temp)
    {
	    sendEvent(name: "temperature", value: json.temp)
   	}
    if (json.mode)
    {
	    if (json.mode == "00") {
			sendEvent(name: "mode", value: "off")
		}elseif(json.mode == "02")	{
			sendEvent(name: "switch", value: "on")
		}else{
			sendEvent(name: "switch", value: "turningOn")
		}
   	}
    if (json.watts)
    {
	    sendEvent(name: "energy", value: json.watts/1000.0)
   	}
    if (json.alltime)
    {
	    sendEvent(name: "duration", value: json.alltime)
   	}
    if (json.times)
    {
	    sendEvent(name: "times", value: json.times)
   	}
}


private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}