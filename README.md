# Irminsul

Opinionated Hibernate ORM wrapper for Java 17+.

I have worked with Spring Boot for some time. When I was working on non-Spring Boot projects, I've been
missing a lot of the database features that Spring Boot and its JPA module provide. So I decided to create
this little library to make my life easier.

As mentioned, this is opinionated library. It is fit to suit my needs - and maybe needs of other developers as well.
It should not be viewed as definitive way to work with Hibernate ORM. I will probably miss out a lot of the features
that it provides.

## Features

- JPA-like repositories with predefined methods
  - Plus some extra methods using the `RepositoryExtension` interface
- ThreadLocal transaction management
- For better or worse, programmatical Hibernate ORM configuration

## Documentation

For documentation on installation and usage, please check [my documentations](https://docs.iwakura.enterprises/irminsul.html).