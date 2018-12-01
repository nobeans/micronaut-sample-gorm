package micronaut.sample.gorm

import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/todo")
class TodoController {

    @Get("/")
    HttpStatus index() {
        return HttpStatus.OK
    }
}
