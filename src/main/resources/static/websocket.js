'use strict'

var connection = null;
function websocket() {

    connection = new WebSocket("ws://localhost:8080/websocket","json");

    connection.onopen = function (evt) {
        console.log("Opened websocket connection");
    };

    connection.onmessage = function(evt) {
        console.log("received message");
        var msg = JSON.parse(evt.data)
        console.log(msg);
        document.getElementById("chat");
    };

    connection.onclose = function(evt) {
        console.log("closed websocket connection");
    };
};

function calculate() {
    console.log("Sending calculation");
    var formula = document.getElementById("text").value;
    var name = document.getElementById("name").value;
    var msg = {
        text: formula,
        type: "message",
        date: Date.now()
    };
    var json = JSON.stringify(msg);
    console.log("json "+ json);
    connection.send(json);
    document.getElementById("text").value = "";
};