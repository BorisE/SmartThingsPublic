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
 *
 * ToDo:
 * 1) Проверка, что чайник отключился (рефреш)
 * 2) Периодический рефреш вообще
 * 3) Промежуточный статус switch
 *
 * 1.06 [04.04.2020]
 * - обнаружен комбо режим работы чайника (boiling and warming)
 * - выводится целевая температура в режиме warming
 * 1.05 [29.03.2020]
 * - reworking 1.03 because of strange bug with 1.04
 * - добавлено отображение времени обновления данных
 * - изменен порядок плиток
 *
 * 1.04 [29.03.2020]
 * - refresh flooding prevention
 * - проверка на пустую строку body response (parse messages)
 */
metadata {
	definition (name: "KettleRedmondDTH", namespace: "BorisE", author: "Boris Emchenko") {
        capability "Switch"						//switch, on(), off()
        capability "Temperature Measurement"	//temperature
        capability "Power Meter"				//power
        capability "Refresh"					//refresh()
        capability "Polling"					//refresh()

        attribute "version", "string"
        attribute "mode", "string"
        //attribute "duration", "number"
        attribute "times", "number"
        attribute "refreshtime", "string"
    }

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
		input name: "about", type: "paragraph", element: "paragraph", title: "Redmond Kettle 1.06", description: "By Boris Emchenko"
    }
    
	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) 
	{
 		standardTile("switch", "device.switch", width: 6, height: 4, decoration: "flat", canChangeIcon: true) {
			state "off", label:'${name}', action:"switch.on", icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#ffffff"
    		state "on", label:'${name}', action:"switch.off", icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#00a0dc"
			state "turningOn", label:'Turning on', icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#00a0dc", nextState: "on"
			state "turningOff", label:'Turning off', icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/appliances3-icn%402x.png", backgroundColor:"#ffffff", nextState: "off"
            }
    	
        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
			state "temperature", label:'${currentValue}°',
			backgroundColors:[
				[value: 35, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			]
		}
		valueTile("refreshtime", "device.refreshtime", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue}'
		}
       
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"refresh.refresh", icon:"https://raw.githubusercontent.com/BorisE/SmartThingsPublic/master/icons/refresh-icon%402x.png"
		}

		valueTile("mode", "device.mode", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue}'
		}

		valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} kWh'
		}
		/*valueTile("duration", "device.duration", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} hours'
		}*/
		valueTile("times", "device.times", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} times'
		}

  		// the "switch" tile will appear in the Things view
        main("switch")
        
        details(["switch", "temperature", "refreshtime", "refresh","mode", "power", "times" ])
	}
}

def installed() {
	log.info  "***installed()*** command call"

	//set default values
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "temperature", value: 0)
	sendEvent(name: "mode", value: "none")
    sendEvent(name: "power", value: 0)
    //sendEvent(name: "duration", value: 0)
    sendEvent(name: "times", value: 0)
    sendEvent(name: "refreshtime", value: "")
}


def on() {
	log.info "***on()*** command call"
    
	sendEvent(name: "switch", value: "turningOn")
    
    runCmd("on")
}

def off() {
	log.info "***off()*** command call"
 	
    sendEvent(name: "switch", value: "turningOff")
 
 	runCmd("off")
}

def refresh() {
	log.info "***Refresh()*** method call"
	
    //update_data()
    
	// Only allow refresh every 10 seconds  to prevent flooding the network
	def timeNow = now()
	if (!state.refreshTriggeredAt || (10 * 1000 < (timeNow - state.refreshTriggeredAt))) {
		state.refreshTriggeredAt = timeNow
		// use runIn with overwrite to prevent multiple DTH instances run before state.refreshTriggeredAt has been saved
		//runIn(2, "update_data", [overwrite: true])
        update_data()
	}
    else
    {
    	log.debug ("Skipping refresh due to flooding prevention")
    }
}

def updated(){
	log.info "***Updated()*** method call"
	/*
	log.trace("Hub address: " + device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP"))
    log.trace("Device IP ${DeviceIP}:${DevicePort}")
    
    update_data()
	*/
	
    def timeNow = now()
    //log.trace (timeNow - state.updatedLastRanAt)
    
    if (!state.updatedLastRanAt || ( 3*1000 < (timeNow - state.updatedLastRanAt)))
    {
		state.updatedLastRanAt = timeNow

        log.trace("Hub address: " + device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP"))
        log.trace("Device IP ${DeviceIP}:${DevicePort}")
		
		def hosthex = convertIPtoHex(DeviceIP).toUpperCase()
		def porthex = convertPortToHex(DevicePort).toUpperCase()
        device.deviceNetworkId = "$hosthex:$porthex"
		log.trace "The device [$DeviceIP:$DevicePort] id configured to: $device.deviceNetworkId"

		//runIn(2, "update_data", [overwrite: true])
        update_data()
	}
	else {
		log.trace "updated(): Ran within last 2 seconds so aborting."
	}
}

def configure(){
	log.info "***configure()*** method call"
}

private def update_data(){

	runCmd("update")

}


private def runCmd(String varCommand) {
	
    log.info("Running command [${varCommand}]")
    
    def cmdPath = ""
	if (varCommand == "update") {
    	cmdPath = "4"
    } else if (varCommand == "on") {
    	cmdPath = "1"
    } else if (varCommand == "off") {
    	cmdPath = "2"
    }
    def randNum = new Random().nextInt(65000) + 1
    def sid = "&sid=" + randNum
    //log.info (sid)

	def hosthex = convertIPtoHex(DeviceIP).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"
	//log.trace "The device [$DeviceIP:$DevicePort] id configured to: $device.deviceNetworkId"
	
	//def userpassascii = "${HTTPUser}:${HTTPPassword}"
	//def userpass = "Basic " + userpassascii.encodeAsBase64().toString()

	def headers = [:]
	headers.put("HOST", "$DeviceIP:$DevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	log.trace "Query header: $headers"
	def method = "GET"

	def path = "/index.php?cmd="+cmdPath+sid
	log.trace "Query path: $DeviceIP:$DevicePort$path"
	def body = ""		
	log.trace "Query body: $body"


	try {
		def hubAction = new physicalgraph.device.HubAction(
			[method: method,
			path: path,
			body: body,
			headers: headers],
			device.deviceNetworkId)
		hubAction.options = [outputMsgToS3:false]
		//log.trace "hubAction is => $hubAction"
		hubAction
		//log.trace "hubAction was run"
	}
	catch (Exception e) {
		log.error "Hit Exception $e on $hubAction"
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
	
    log.info "Parsing message: '${description}'"
    
    def parsedEvent= parseEventMessage( description)
	//log.debug "AfterParsingEventMessage '${parsedEvent}'"

    def headerString = new String(parsedEvent.headers.decodeBase64())
    log.trace "Response header: '${headerString}'"
    
    def json
    if (parsedEvent?.body?.decodeBase64())
    {
    	def bodyString = new String(parsedEvent?.body?.decodeBase64())
	    log.trace "Response body '${bodyString}'"
	    json = new groovy.json.JsonSlurper().parseText( bodyString)
		log.debug (json) // {alltime=13.7, durat=7f, mode=00, now=2020-03-29 23:57:20, runtime=4.87, status=00, targettemp=100, temp=72, times=357, watts=30162}
    }
    else
    {
	    log.warn "Response body is empty"
    }
    
    if (json?.status) //#may be '00' - OFF or '02' - ON
    {
	    //log.trace "status: '${json.status}'"
        if (json.status == "00") {
			sendEvent(name: "switch", value: "off")
		}else if(json.status == "02")	{
			sendEvent(name: "switch", value: "on")
		}else{
			sendEvent(name: "switch", value: "turningOn")
		}
   	}
    if (json?.temp)
    {
	    sendEvent(name: "temperature", value: json.temp)
   	}
    if (json?.mode) //# '00' - boil, '01' - heat to temp, '03' - backlight
    {
	    if (json.mode == "00") {
			sendEvent(name: "mode", value: "boil")
		}else if(json.mode == "01")	{
            sendEvent(name: "mode", value: "keep at "+json?.targettemp+"°")
		}else if(json.mode == "02")	{
			sendEvent(name: "mode", value: "boil&keep at "+json?.targettemp+"°") //обнаружил эмпирически - можно установить через приложение и комбо режим
		}else if(json.mode == "03")	{
			sendEvent(name: "mode", value: "backlight")
		}else{
			sendEvent(name: "mode", value: "? ("+json.mode+")")
		}
   	}
    if (json?.watts)
    {
	    sendEvent(name: "power", value:  Math.round (json.watts/1000*10)/10 )
   	}
    /*
    if (json?.alltime)
    {
	    sendEvent(name: "duration", value: json.alltime)
   	}*/
    if (json?.times)
    {
	    sendEvent(name: "times", value: json.times)
   	}
    if (json?.now)
    {
	    sendEvent(name: "refreshtime", value: json.now)
	    //sendEvent(name: "refreshtime", value: "'${json.now}' | '${state.refreshTriggeredAt}'")
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