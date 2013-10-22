$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    $("#messages").append $("<li>").text(message.message)
  
  # when the user hits the send button, send the message over the websocket
  $("#message-form").submit (event) ->
    event.preventDefault()
    message = message: $("#message-text").val()
    ws.send(JSON.stringify(message))
    $("#message-text").val("")