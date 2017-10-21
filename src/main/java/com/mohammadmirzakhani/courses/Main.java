package com.mohammadmirzakhani.courses;

import com.google.gson.Gson;
import com.mohammadmirzakhani.courses.enams.StatusResponse;
import com.mohammadmirzakhani.courses.interfaces.UserService;
import com.mohammadmirzakhani.courses.model.StandardResponse;
import com.mohammadmirzakhani.courses.model.User;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.omg.CORBA.UserException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static spark.Spark.*;

/**
 * Created by Mohammad Mirzakhani on 10/6/17.
 */
public class Main {

    private static UserService userService;

    //Hold reusable refrence to SessionFactory (Since we need only one)
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        final ServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
        return new MetadataSources(registry).buildMetadata().buildSessionFactory();

    }


    public static void main(String[] args) {

//row - json
        post("/users", (request, response) -> {
            response.type("application/json");
            User user = new Gson().fromJson(request.body(), User.class);

            userService.addUser(user);

            return new Gson()
                    .toJson(new StandardResponse(StatusResponse.SUCCESS));
        });

        //x-www-form
        post("/addUsers", (request, response) -> {
            userService.addUser(
                    new User(
                            request.queryParams("firstName"),
                            request.queryParams("lastName"),
                            request.queryParams("email")
                    )
            );
            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS));
        });


        get("/users", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(
                    new StandardResponse(StatusResponse.SUCCESS, new Gson()
                            .toJsonTree(userService.getUsers())));
        });


        get("/users/:id", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(
                    new StandardResponse(StatusResponse.SUCCESS, new Gson()
                            .toJsonTree(userService.getUser(Integer.parseInt(request.params(":id"))))));
        });

//        delete("/users/:id", (request, response) -> {
//            response.type("application/json");
//            userService.deleteUser(Integer.parseInt(request.params(":id")));
//            return new Gson().toJson(
//                    new StandardResponse(StatusResponse.SUCCESS, "user deleted"));
//        });


        options("/users/:id", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(
                    new StandardResponse(StatusResponse.SUCCESS,
                            (userService.userExist(
                                    Integer.parseInt(request.params(":id")))) ? "User exists" : "User does not exists"));
        });


        userService = new UserService() {
            ArrayList<User> users = new ArrayList<>();

            @Override
            public void addUser(User user) {
                save(user);
                users.add(user);
            }

            @Override
            public Collection<User> getUsers() {
                return fetchAllContacts();
            }

            @Override
            public User getUser(int id) {
                return findContactById(id);
            }

            @Override
            public User editUser(User user) throws UserException {
                return null;
            }

            @Override
            public void deleteUser(int id) {
                Collection<User> allUsers = getUsers();
                for (User user : allUsers) {
                    if (user.getId() == id) {
                        users.remove(user);
                        delete(user);
                    }
                }

            }

            @Override
            public boolean userExist(int id) {
                Collection<User> allUsers = getUsers();
                for (User user : allUsers) {
                    if (user.getId() == id) {
                        return true;
                    }
                }
                return false;
            }

        };
    }

    private static int save(User user) {

        //Open a session
        Session session = sessionFactory.openSession();

        //Begin a transaction
        session.beginTransaction();

        //Use the session to save the contact
        int id = (int) session.save(user);

        //Commit the transaction
        session.getTransaction().commit();

        //close the session
        session.close();

        return id;
    }

    @SuppressWarnings("unchecked")
    private static List<User> fetchAllContacts() {
        //Open session
        Session session = sessionFactory.openSession();
        //Create Criteria
        Criteria criteria = session.createCriteria(User.class);
        List<User> userList = criteria.list();
        //close the session
        session.close();

        return userList;
    }


    private static User findContactById(int id) {
        Session session = sessionFactory.openSession();

        //Retrieve the persistent object (or null if not found)
        User user = session.get(User.class, id);
        session.close();

        return user;
    }

    private static void delete(User user) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        session.delete(user);

        session.getTransaction().commit();
        session.close();
    }

}
