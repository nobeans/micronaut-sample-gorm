package micronaut.sample.gorm

import grails.gorm.transactions.Rollback
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Rollback
class TodoSpec extends Specification {

    // TODO これがないと、GORMの初期化が実行されずエラーになってしまう。違う方法でGORMの初期化をする方法はないものか。
    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

    def "save/count/get"() {
        given:
        assert Todo.count() == 0

        when:
        def todo = new Todo(title: "TEST_TODO", deadline: "today").save(flush: true)

        then:
        !todo.hasErrors()
        Todo.count() == 1
        Todo.get(todo.id).title == "TEST_TODO"
    }

    def "save as updating"() {
        given:
        def todo = new Todo(title: "TEST_TODO", deadline: "today").save(flush: true)
        assert Todo.count() == 1

        when:
        todo.title = "UPDATED"

        and:
        todo.save(flush: true)

        then:
        Todo.count() == 1
        Todo.get(todo.id).title == "UPDATED"
    }

    def "delete/count/get"() {
        given:
        def todo = new Todo(title: "TEST_TODO", deadline: "today").save(flush: true)
        assert Todo.count() == 1

        when:
        todo.delete(flush: true)

        then:
        Todo.count() == 0
        Todo.get(todo.id) == null
    }

    def "list"() {
        given:
        titles.each { String title ->
            new Todo(title: title, deadline: "today").save(flush: true)
        }

        expect:
        Todo.list()*.title == expectedTitles

        where:
        titles          | expectedTitles
        []              | []
        ["A"]           | ["A"]
        ["A", "B", "C"] | ["A", "B", "C"]
    }

    @Unroll
    def "validate: field #field.inspect() is #valid when value is #value.inspect()"() {
        given:
        def todo = new Todo()
        todo[field] = value

        when:
        todo.validate()

        then:
        todo.errors[field]?.code == (valid == "valid" ? null : valid)

        where:
        field      | valid              | value
        "title"    | "nullable"         | null
        "title"    | "blank"            | ""
        "title"    | "valid"            | "TEST_TODO"
        "title"    | "valid"            | "x" * 500
        "title"    | "maxSize.exceeded" | "x" * 501
        "deadline" | "nullable"         | null
        "deadline" | "valid"            | ""
        "deadline" | "valid"            | "today"
        "deadline" | "valid"            | "tomorrow"
        "deadline" | "valid"            | "x" * 100
        "deadline" | "maxSize.exceeded" | "x" * 101
    }
}
