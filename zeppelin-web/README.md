# Zeppelin Web Application
This is a Zeppelin web frontend project.


## Compile Zeppelin web
If you want to compile the WebApplication, you will have to simply run `mvn package`.
This will Download all the dependencies including node js and npm 
(you will find the binaries in the folder `zeppelin-web/node`).

We also provide some **helper script** for bower and grunt 
(you dont need to install them).

In case of the error 
`ECMDERR Failed to execute "git ls-remote --tags --heads git://xxxxx", exit code of #128`

change your git config with `git config --global url."https://".insteadOf git://`

**OR**

Try to add to the `.bowerrc` file the following content:
```
  "proxy" : "http://<host>:<port>",
  "https-proxy" : "http://<host>:<port>"
```

and retry to build again.

## Contribute on Zeppelin Web
If you wish to help us to contribute on Zeppelin Web, you will need to install some deps.
Here this is a good start to understand how zeppelin web is architectured.
http://www.sitepoint.com/kickstart-your-angularjs-development-with-yeoman-grunt-and-bower/

Also, Zeppelin uses Typescript http://www.typescriptlang.org/ to maintain 
stable structure of javascript codes . 
You can read a good article on using AngularJs with Typescript in the next link. 
http://www.scottlogic.com/blog/2014/08/26/StrongTypingWithAngularJS.html
 
### Run the application in dev mode
``./grunt serve``

### Build the application
`./grunt build`

### Add components to Zeppelin Webapp
 * New controller : `yo angular:controller <controlerName>`
 * New directive : `yo angular:directive <directiveName>`
 * New service : `yo angular:service <serviceName>`
 
(Currently, adding components by Yeoman (yo) is broken, 
because officially Yeoman doesn't support Typescript.)

You can generate codes by manually installing yo and linking generator. 
reference: https://github.com/yeoman/yeoman/blob/master/contributing.md

generator supporting Typescript:
https://github.com/eggers/generator-angular

command: 
`yo angular:controller <name> --typescript`

(Need test if a generated codes works well.)

 ### Add plugin
 
 `./bower install <plugin> -save`
 update the file index.html with the new bower_components 
 
 ex: `./bower install angular-nvd3` 
 ```
 <script src="bower_components/angular-nvd3/dist/angular-nvd3.js"></script>
 ````




