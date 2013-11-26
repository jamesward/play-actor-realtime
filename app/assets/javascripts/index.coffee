# On page load
$ ->

  # Helper to get the WS URL or one that doesn't work
  wsUrl = () ->
    if $("#ws-enabled").prop("checked")
      $("body").data("ws-url")
    else
      # If the WS is disabled use a url that will not work
      "ws://locahost:9000/not-gonna-work"
  
  # Helper to get the es URL
  esUrl = () ->
    $("body").data("es-url")


  # initiate a connection now
  channel = new Channel(wsUrl(), esUrl())

  # Optionally handle the open event on the channel
  #channel.onopen = (event) ->
  #  console.debug("wahoo!")
  
  # Optionally handle receiving a message over the channel
  channel.onmessage = (message) ->
    data = JSON.parse message.data
    $("#messages").append $("<li>").text("Receieved: " + data.message)

  # Send a message over the channel when the user clicks the Send button
  $("#message-form").submit (event) ->
    event.preventDefault()
    message = message: $("#message-text").val()
    channel.send(JSON.stringify(message))
    $("#message-text").val("")
  
  # When the "Enable WS" checkbox is checked or unchecked, reconnect
  $("#ws-enabled").change (event) ->
    channel.reconnect(wsUrl(), esUrl())


#
# A Channel abstraction over WebSocket and es
#
class Channel
  constructor: (wsUrl, esUrl) ->
    @reconnect(wsUrl, esUrl)
    
  wsUrl: null
  ws: null
  
  esUrl: null
  es: null
  
  onopen: (event) ->
    # default open event handler
    console.debug("Channel Open", event)
  
  onmessage: (message) ->
    #default message event handler
    console.debug("Channel Message", message)

  send: (message) ->
    if (@ws.readyState == WebSocket.OPEN)
      # sending through the WebSocket
      console.debug("WebSocket Send", message)
      @ws.send(message)
    else
      console.debug("AJAX Send", message)
      # sending with a plain 'ole post
      $.ajax
        url: @esUrl
        type: 'post'
        dataType: 'json'
        contentType: 'application/json'
        data: message
  
  reconnect: (_wsUrl, _esUrl) ->
    self = @
    
    @wsUrl = _wsUrl
    @esUrl = _esUrl
    
    console.debug("Trying to connect to the WebSocket", @wsUrl)
    
    @ws = new WebSocket(@wsUrl)
    @ws.onopen = (event) ->
      console.debug("WebSocket Open", event)
      self.onopen(event)
    @ws.onmessage = (message) ->
      console.debug("WebSocket Message", message)
      self.onmessage(message)
    @ws.onerror = (event) ->
      console.debug("WebSocket Error", event)
      if (event.target.readyState == WebSocket.CLOSED)
        self.fallback()
  
  fallback: () ->
    self = @
    
    console.debug("Falling back to EventSource", @esUrl)
    
    es = new EventSource(@esUrl)
    es.onopen = (event) ->
      console.debug("EventSource Open", event)
      self.onopen(event)
    es.onmessage = (message) ->
      console.debug("EventSource Message", message)
      self.onmessage(message)
    