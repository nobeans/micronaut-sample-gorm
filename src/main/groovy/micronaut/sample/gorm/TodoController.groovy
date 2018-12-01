package micronaut.sample.gorm

import grails.gorm.transactions.Transactional
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

// TODO readonly=trueにしても、save/deleteでしれっと更新できてしまう??
@Transactional
@Controller("/todo")
class TodoController {

    @Get
    List<Todo> index() {
        Todo.list()
    }

    @Get("/{id}")
    Todo show(Serializable id) {
        Todo.get(id)
    }

    @Post
    HttpResponse<Todo> save(@Body Todo todo) { // TODO typeMismatch的なエラーをどうやってキャッチするか
        if (!todo.save(flush: true, failOnError: false)) { // TODO failOnError:falseを明示指定しないとtrue扱い？
            return HttpResponse.unprocessableEntity().body(todo) as HttpResponse<Todo>
        }
        HttpResponse.created(todo)
    }

    @Put("/{id}")
    HttpResponse<Todo> update(Serializable id, @Body Todo updatedTodo) {
        def todo = Todo.get(id)
        if (!todo) {
            return HttpResponse.notFound()
        }

        todo.title = updatedTodo.title
        todo.deadline = updatedTodo.deadline
        if (!todo.save(flush: true, failOnError: false)) {
            return HttpResponse.unprocessableEntity().body(todo) as HttpResponse<Todo>
        }
        HttpResponse.ok(todo)
    }

    @Delete("/{id}")
    void delete(Serializable id) {
        Todo.get(id)?.delete(flush: true)
    }
}
