
var win, dataview, clearbutt, recordbutt, savebutt, openbutt, postbutt, playbutt, connectOSCbutt, ipAddressfield, numberfield;
var recording, oscPlayback, timeOffset;
var oscresponder, oscList, oscSender, oscFunc;
var file, postFlag;

postFlag = true;
recording = false;
oscSender = NetAddr("127.0.0.1", 7110);
timeOffset = Main.elapsedTime;

oscFunc = { |msg, time, addr | 
		if(msg[0] != '/status.reply') {
			if(postFlag, {[time-timeOffset, msg].postln });
			if(recording, {
				//oscList.add([time-timeOffset, msg]); // this works for Pd (OSC timestamp)
				oscList.add([Main.elapsedTime-timeOffset, msg]); // this works for OSCeleton (timestamped in SC)
				// {dataview.string_(oscList.asCompileString)}.defer;
			});
		}  

	};

/*
oscresponder = OSCresponderNode(nil, '/test', { |time, r, msg| 
	//[time, msg].postln;
	if(recording, {
		oscList.add([time-timeOffset, msg]); 
		{dataview.string_(oscList.asCompileString)}.defer;
	});
}).add;
*/

thisProcess.addOSCRecvFunc(oscFunc);


oscList = List.new;

oscPlayback = Task({
	var timekeep = 0;
	oscList.do({ | event |
		var deltatime, msg;
		deltatime = event[0];
		msg = event[1];
		oscSender.sendMsg(*msg); // unpack array to individual arguments
		if(postFlag, { msg.postln });
		//oscSender.sendMsg(msg);
		//"Sending OSC : ".post; msg.postln;
		(deltatime-timekeep).wait;
		timekeep = deltatime;
	});
	{playbutt.value_(0)}.defer;
	{oscPlayback.reset}.defer(1);
});

win = Window.new("OSC data recorder", Rect(100, 200, 520, 650));

dataview = TextView(win, Rect(10, 10, 500, 500)).hasVerticalScroller_(true);

recordbutt = Button(win, Rect(10, 520, 150, 30))
				.states_([
					["Record OSC", Color.black, Color.green(alpha:0.1)],
					["Stop Recording", Color.black, Color.red(alpha:0.1)]
				])
				.action_({ arg butt;
					if(butt.value == 1, {
						timeOffset = Main.elapsedTime;
						oscList = List.new;
						recording = true;
						"recording is true".postln;	
					}, {
						recording = false;	
						{dataview.string_(oscList.asCompileString)}.defer;
					});
				});

postbutt = Button(win, Rect(10, 560, 150, 30))
				.states_([
					["Posting", Color.black, Color.green(alpha:0.1)],
					["Posting Off", Color.black, Color.grey(alpha:0.1)]
				])
				.action_({ arg butt;
					if(butt.value == 1, {
						postFlag = false;
						"post is false".postln;	
					}, {
						postFlag = true;
						"post is true".postln;	
					});
				});


clearbutt = Button(win, Rect(180, 520, 150, 30))
				.states_([
					["Clear Window", Color.black, Color.clear]
				])
				.action_({ arg butt;
					dataview.string_("");	
				});


playbutt = Button(win, Rect(350, 520, 150, 30))
				.states_([
					["Play OSC", Color.black, Color.green(alpha:0.1)],
					["Stop OSC", Color.black, Color.red(alpha:0.1)]
				])
				.action_({ arg butt;
					if(butt.value == 1, {
						oscPlayback.play;
						"playing is true".postln;	
					}, {
						//oscPlayback.reset;
						oscPlayback.stop;	
					});
				});

openbutt = Button(win, Rect(180, 560, 150, 30))
				.states_([
					["Open File", Color.black, Color.clear]
				])
				.action_({ arg butt;
					//oscList = Object.readArchive("test.osc");

					Dialog.getPaths({ arg path;
						path.postln;
						oscList = Object.readArchive(path[0]);
						dataview.string_(oscList.asCompileString);
					},{
						"cancelled".postln;
					}, false);
				

				});

savebutt = Button(win, Rect(350, 560, 150, 30))
				.states_([
					["Save File", Color.black, Color.clear]
				])
				.action_({ arg butt;
					Dialog.savePanel({ arg path;
						path.postln;
						if(path.splitext[1] == "osc", {
							oscList.writeArchive(path);
						}, {
							oscList.writeArchive(path++".osc");
						});
					},{
						"cancelled".postln;
					});
					//oscList.writeArchive("test.osc");
				});

connectOSCbutt = Button(win, Rect(10, 600, 150, 30))
				.states_([
					["Connect OSC", Color.black, Color.clear]
				])
				.action_({ arg butt;
					oscSender = NetAddr(ipAddressfield.string, numberfield.string.asInteger);
					"Connected OSC".postln;
				});

ipAddressfield = TextView(win, Rect(180, 605, 150, 20))
				.font_(Font("Helvetica", 14))
				.string_(oscSender.ip.asString)
				.hasVerticalScroller_(false);

numberfield = TextView(win, Rect(350, 605, 150, 20))
				.font_(Font("Helvetica", 14))
				.string_(oscSender.port.asString)
				.hasVerticalScroller_(false);

win.front;
win.onClose_({
	thisProcess.removeOSCRecvFunc(oscFunc);

});



