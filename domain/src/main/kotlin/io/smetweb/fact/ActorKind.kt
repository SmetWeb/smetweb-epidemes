package io.smetweb.fact

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.smetweb.ref.NameRef

/**
 * An [ActorKind] can participate in exchanges of [ResultExchange] patterns
 * with coordination [CoordinationFact]s for specific [ResultKind]s
 */
interface ActorKind: Observer<CoordinationFact> {

	fun name(): NameRef

	fun emitFacts(): Observable<CoordinationFact> = Observable.empty()

	override fun onSubscribe(d: Disposable) {
	}

	override fun onNext(t: CoordinationFact) {
	}

	override fun onError(e: Throwable) {
	}

	override fun onComplete()

	fun emit(factKind: FactKind): Observable<CoordinationFact> = emitFacts()
			.filter { fact -> fact.getKind() == factKind }

	fun emit(resultKind: ResultKind): Observable<CoordinationFact> = emitFacts()
			.filter { fact -> fact.getExchange().getResultKind() == resultKind }

}