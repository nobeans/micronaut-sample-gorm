package micronaut.sample.gorm

import grails.gorm.transactions.Rollback
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Rollback
class TodoControllerSpec extends Specification {

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

    @Shared
    @AutoCleanup
    RxHttpClient client = embeddedServer.applicationContext.createBean(RxHttpClient, embeddedServer.getURL())

    def cleanup() {
        deleteAllTodo()
    }

    def "index: TODOが空の場合は空のリストを返す"() {
        when:
        def todoList = client.toBlocking().retrieve("/todo", List)

        then:
        todoList.size() == 0
    }

    def "index: TODOが登録済みの場合はすべてのTODOのリストを返す"() {
        given:
        saveTodo(3)

        when:
        def todoList = client.toBlocking().retrieve("/todo", List)

        then:
        todoList*.title == ["TEST_TITLE_0", "TEST_TITLE_1", "TEST_TITLE_2"]
    }

    def "show: 指定したTODOの情報を返す"() {
        given:
        def todoList = saveTodo(3)

        when:
        def todo = client.toBlocking().retrieve("/todo/${todoList.first().id}", Todo) as Todo

        then:
        todo.title == "TEST_TITLE_0"
    }

    def "show: 対象が見つからない場合は404 Not Foundを返す"() {
        when:
        client.toBlocking().exchange("/todo/not_found")

        then:
        def e = thrown(HttpClientResponseException)
        e.status == HttpStatus.NOT_FOUND
        e.message == "Page Not Found"
    }

    def "save: 新しいTODOを登録する"() {
        when:
        client.toBlocking().exchange(HttpRequest.POST("/todo", new Todo(title: "TEST_TITLE")))

        then:
        client.toBlocking().retrieve("/todo", List)*.title == ["TEST_TITLE"]
    }

    def "save: バリデーションがNGの場合は422 Unprocessable Entityを返す"() {
        when:
        client.toBlocking().exchange(HttpRequest.POST("/todo", new Todo(title: "x" * 101)))

        then:
        def e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.message == "Unprocessable Entity"
    }

    def "update: 登録済みのTODOを更新する"() {
        given:
        def todoList = saveTodo(1)
        def todo = client.toBlocking().retrieve("/todo/${todoList.first().id}", Todo) as Todo
        todo.title = "UPDATED"

        when:
        client.toBlocking().exchange(HttpRequest.PUT("/todo/${todo.id}", todo))

        then:
        client.toBlocking().retrieve("/todo", List)*.title == ["UPDATED"]
    }

    def "update: バリデーションがNGの場合は422 Unprocessable Entityを返す"() {
        given:
        def todoList = saveTodo(1)
        def todo = client.toBlocking().retrieve("/todo/${todoList.first().id}", Todo) as Todo
        todo.title = "x" * 101

        when:
        client.toBlocking().exchange(HttpRequest.PUT("/todo/${todo.id}", todo))

        then:
        def e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNPROCESSABLE_ENTITY
        e.message == "Unprocessable Entity"
    }

    def "delete: 登録済みのTODOを削除する"() {
        given:
        def todoList = saveTodo(3)
        def targetId = todoList.first().id

        when:
        client.toBlocking().exchange(HttpRequest.DELETE("/todo/$targetId"))

        then:
        def afterTodoList = client.toBlocking().retrieve("/todo", List)
        afterTodoList.size() == todoList.size() - 1
        !afterTodoList*.id.contains(targetId)
    }

    def "delete: 削除対象が存在しない場合でも200 OKを返す"() {
        when:
        def response = client.toBlocking().exchange(HttpRequest.DELETE("/todo/not_found"))

        then:
        response.status == HttpStatus.OK
    }

    private List<Map> saveTodo(int number) {
        number.times {
            client.toBlocking().exchange(HttpRequest.POST("/todo", new Todo(title: "TEST_TITLE_$it")))
        }
        client.toBlocking().retrieve("/todo", List)
    }

    private void deleteAllTodo() {
        (client.toBlocking().retrieve("/todo", List)).each { Map todo ->
            client.toBlocking().exchange(HttpRequest.DELETE("/todo/${todo.id}"))
        }
    }
}
