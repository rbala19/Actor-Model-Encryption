package ActorModel.Actors;

import ActorModel.BatchUtilities;
import ActorModel.Supervisor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;

import java.util.LinkedList;
import java.util.List;


/**
 * Created by rbalakrishnan on 7/28/17.
 */
public class SelectActor extends AbstractActor{

	List<ActorRef> updateActors;
	int movingSize;

	public static Props props() {
		return Props.create(SelectActor.class, () -> new SelectActor());
	}

	public SelectActor() {
		updateActors = new LinkedList<>();
		movingSize = 0;
	}

	public Receive createReceive() {
		return receiveBuilder()
				.match(BatchUtilities.Batch.class, batch -> {
					BatchUtilities.Batch encryptedBatch = encryptBatch(batch);

					int mod = Supervisor.getLastInt(getSelf().path().name());
					ActorRef next = getContext().actorOf(UpdateActor.props(), String.format("UpdateActor%d", Supervisor.branchingFactor * movingSize + mod));
					this.getContext().watch(next);

					updateActors.add(next);
					movingSize++;

					next.tell(encryptedBatch, getSelf());
				})
				.match(Terminated.class, t -> {
//					updateActors.remove(t.getActor());
				})
				.build();

	}

	private BatchUtilities.Batch encryptBatch(BatchUtilities.Batch batchToEncrypt) {
		return batchToEncrypt.encryptBatch(batchToEncrypt.getRequest().getEncryptFlag());
	}




}
