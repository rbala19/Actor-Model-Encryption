package ActorModel.Actors;

import ActorModel.BatchUtilities;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.routing.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rbalakrishnan on 7/28/17.
 */

/**
 * Highest level of Actor Hierarchy; Receives EncryptRequests and outputs batches to each SelectActor
 */
public class BatchActor extends AbstractActor {

	Router router;

	List<Routee> routees = new ArrayList<>();

	public int numChildActors;

	public BatchActor(int numChildActors) {

		this.numChildActors = numChildActors;

		//Add SelectActor children to router
		for (int i = 0; i < numChildActors; i++) {
			ActorRef r = getContext().actorOf(SelectActor.props(), "SelectActor" + i);
			getContext().watch(r);
			routees.add(new ActorRefRoutee(r));
		}
		//Sends messages in a round robin method (going around the circle)
		router = new Router(new RoundRobinRoutingLogic(), routees);
	}



	public static Props props(int numChild) {
		return Props.create(BatchActor.class, () -> new BatchActor(numChild));
	}

	/**
	 * If a request is received, batches are sent to select actors
	 *
	 * If a Select Actor dies (because of a failure), a new one is added to the router.
	 *
	 */
	public Receive createReceive() {
		return receiveBuilder()
			.match(BatchUtilities.EncryptRequest.class, request -> {
				int numBatches = request.getNumBatches();
				for (int i = 0; i < numBatches; i++) {
					router.route(request.createBatch(), getSender());
				}
			})
			.match(Terminated.class, message -> {
				router = router.removeRoutee(message.actor());
				ActorRef r = getContext().actorOf(Props.create(SelectActor.class));
				getContext().watch(r);
				router = router.addRoutee(new ActorRefRoutee(r));
			})
			.build();



	}


}
