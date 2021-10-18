package io.smetweb.fact

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.smetweb.refer.NameRef

/**
 * An [ActorKind] can participate in exchanges of [ResultExchange] patterns
 * with coordination [CoordinationFact]s for specific [ResultKind]s
 */
interface ActorKind: Observer<CoordinationFact> {

	val name: NameRef

	val factSource: Observable<CoordinationFact>
			get() = Observable.empty()

	override fun onSubscribe(d: Disposable) {
		/* provide empty default body */
	}

	override fun onNext(t: CoordinationFact) {
		/* provide empty default body */
	}

	override fun onError(e: Throwable) {
		/* provide empty default body */
	}

	override fun onComplete() {
		/* provide empty default body */
	}

	fun emit(factKind: FactKind): Observable<CoordinationFact> =
			this.factSource.filter { fact -> fact.kind == factKind }

	fun emit(resultKind: ResultKind): Observable<CoordinationFact> =
			this.factSource.filter { fact -> fact.exchange.resultKind == resultKind }

}