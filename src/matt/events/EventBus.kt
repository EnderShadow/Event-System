package matt.events

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue

object EventBus
{
    private lateinit var eventQueue: PriorityBlockingQueue<Event>
    
    private val listeners = ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<EventListener, Boolean>>()
    
    @Synchronized
    fun init(preferHigherPriority: Boolean)
    {
        if(EventBus::eventQueue.isInitialized)
            throw IllegalStateException("Cannot initialize the event system after it has already been initialized.")
        eventQueue = if(preferHigherPriority)
        {
            PriorityBlockingQueue(100, Comparator {e1, e2 ->
                e1.priority - e2.priority
            })
        }
        else
        {
            PriorityBlockingQueue(100, Comparator {e1, e2 ->
                e2.priority - e1.priority
            })
        }
        
        val eventThread = Thread {
            while(true)
            {
                try
                {
                    val event = eventQueue.take()
                    listeners[event.eventType]?.forEach {it.handleEvent(event)}
                }
                catch(e: Exception)
                {
                    System.err.println("An exception has occurred while handling an event")
                    e.printStackTrace()
                }
            }
        }
        eventThread.isDaemon = true
        eventThread.name = "EventBus Thread"
        eventThread.start()
    }
    
    @Synchronized
    fun postEvent(eventType: String, priority: Int, payload: Any?)
    {
        if(!EventBus::eventQueue.isInitialized)
            throw IllegalStateException("Cannot post an event to the event bus before it is initialized")
        
        if(eventType !in listeners.keys)
        {
            System.err.println("An event has been posted for the following event type, which does not have a listener: $eventType")
            return
        }
        eventQueue.offer(Event(eventType, priority, payload))
    }
    
    @Synchronized
    fun registerListener(eventType: String, listener: EventListener)
    {
        listeners.getOrPut(eventType) {ConcurrentHashMap.newKeySet()}.add(listener)
    }
    
    @Synchronized
    fun unregisterListener(eventType: String, listener: EventListener)
    {
        listeners[eventType]?.remove(listener)
    }
}