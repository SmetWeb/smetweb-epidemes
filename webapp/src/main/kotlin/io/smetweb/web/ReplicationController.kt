package io.smetweb.web

import io.reactivex.Single
import io.smetweb.log.getLogger
import io.vertx.reactivex.core.Vertx
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import reactor.adapter.rxjava.RxJava2Adapter.*
import reactor.core.publisher.Mono
import java.security.Principal

// see https://www.baeldung.com/spring-security-5-reactive#1-bootstrapping-the-reactive-application
@RestController
//@RequestMapping("/replications")
class ReplicationController(
        @Autowired
        private val rxVertx: Vertx
) {

    private val log = getLogger()

    @GetMapping("/")
    fun greet(principal: Mono<Principal>): Mono<String> =
            singleToMono(
                    monoToFlowable(principal)
                            .map(Principal::getName)
                            .single("<anonymous>")
                            .flatMap(this::getResponse))

    fun getResponse(name: String): Single<String> =
            rxVertx.eventBus()
                    .rxRequest<String>("do.something", name)
                    .doOnSuccess { log.info("Reply for name '{}': {}", name, it.body()) }
                    .doOnError { log.warn("Could not handle name '{}': {}", name, it.message, it) }
                    .map { msg -> "Hello, ${msg.body()}" }

    @GetMapping("/admin")
    fun greetAdmin(principal: Mono<Principal>): Mono<String> {
        return principal
                .map(Principal::getName)
                .map { "Admin access: $it" }
    }

//    @GetMapping
//    private fun getAllEmployees(): Flux<Employee?>? {
//        return employeeRepository.findAllEmployees()
//    }

//    @GetMapping("/{id}")
//    private fun getEmployeeById(@PathVariable id: String): Mono<Employee?>? {
//        return employeeRepository.findEmployeeById(id)
//    }

//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping("/update")
//    private fun updateEmployee(@RequestBody employee: Employee): Mono<Employee?>? {
//        return employeeRepository.updateEmployee(employee)
//    }


}