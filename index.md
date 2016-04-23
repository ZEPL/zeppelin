---
layout: page
title: Zeppelin
tagline: Supporting tagline
---
{% include JB/setup %}

<ul class="posts">
    <div class="row">
         <div class="col-sm-4">
             <!-- AWS Zeppelin Demo -->
             <div class="alert alert-info" style="text-align:center;margin:0 0 30px 0;padding-top:30px;">
                 <h3 class="post-title"><a>Try Zeppelin</a></h3>
                 <div style="margin:10px 0 10px 0px" class="btn btn-info"
                      onclick="ga('send', 'event', 'Zeppelin', 'Start', 'Homepage btn'); location.href='/demo.html';">Start</div>
                 <p>Live demo powered by Apache Spark and Zeppelin on Amazon EMR</p>
                 <a href="https://aws.amazon.com/" target="_blank"><img src="{{ ASSET_PATH }}/img/aws-logo.jpg" style="width:140px"></img></a>
             </div>
         </div>
         <div class="col-sm-8">
             <!-- Intro -->
             <p style="font-size:16px"><b>Apache Zeppelin (incubating)</b> is a web-based notebook that enables interactive data analytics. 
You can make beautiful data-driven, interactive and collaborative documents with SQL, Scala and more.
             </p>
             <p style="font-size:16px;">
                 Official Website for Apache Zeppelin (incubating) is <a href="http://zeppelin.incubator.apache.org">http://zeppelin.incubator.apache.org</a>
             </p>

             <!-- Download -->
             <div style="text-align:center">
                <div class="row">
                    <div class="col-sm-6">
                        <div style="height:130px">
                            <img src="{{ ASSET_PATH }}/img/zeppelin-logo.svg" style="width:100px;margin-top:30px"></img>
                        </div>
                        <div style="font-size:20px;color:#555555"><b>Download Zeppelin</b></div>
                        <a href="http://www.apache.org/dyn/closer.cgi/incubator/zeppelin/0.5.6-incubating/zeppelin-0.5.6-incubating-bin-all.tgz" target="_blank">0.5.6 Official release</a> |
                        <a href="http://home.apache.org/~moon/zeppelin/snapshots/zeppelin-0.6.0-incubating-SNAPSHOT-bin-all.tgz">0.6.0 Snapshot</a>
                    </div>
                    <div class="col-sm-6">
                        <div id="shell">
                              <img src="{{ ASSET_PATH }}/img/zeppelin-logo.svg" width="50" height="50" id="wheel1"/>
                              <img src="{{ ASSET_PATH }}/img/zeppelin-logo.svg" width="65" height="65" id="wheel2"/>
                              <img src="{{ ASSET_PATH }}/img/zeppelin-logo.svg" width="80" height="80" id="wheel3"/>
                              <img src="{{ ASSET_PATH }}/img/zeppelin-logo.svg" width="40" height="40" id="wheel4"/>
                        </div>
                        <div style="font-size:20px;color:#555555"><img src="{{ ASSET_PATH }}/img/zeppelinhub-logo.png" style="margin-right:5px;width:30px;"/><b>ZeppelinHub</b></div>
                        <a href="https://www.zeppelinhub.com" target="_blank">Hub</a> |
                        <a href="https://www.zeppelinhub.com/viewer" target="_blank">Viewer</a> |
                        <a href="http://help.zeppelinhub.com" target="_blank">Learn more</a>
                    </div>
                </div>
             </div>
             <br />
             <!-- apache trademark -->
             <div style="text-align:center">
                <div class="row">
                    <div class="col-sm-12">
                        <div style="margin-top:10px;text-align:right;font-size:10px;color:#777777">Apache Zeppelin and the Zeppelin logo are trademarks of the Apache Software Foundation</div>
                    </div>
                </div>
             </div>
         </div>
    </div>
    <hr>
    <div class="row">
        <!-- articles -->
        <div class="col-sm-8">
            <div>
            </div>
            <br /><br />
            <ul class="posts">
                {% for post in site.posts %}
                <li class="post-li">
                <div class="row">
                    <aside class="col-sm-2">
                            <ul>
                                <li><h4>{{ post.date | date_to_string }}</h4></li>
                                <li>{{ post.tags | array_to_sentence_string }}</li>
                            </ul>
                    </aside>
                    <div class="col-sm-10">
                    <h3 class="post-title"> <a href="{{ BASE_PATH }}{{ post.url }}">{{ post.title }}</a> </h3>
                            <span>{% if post.content contains "<!--more-->" %} {{ post.excerpt }} {% else %} {{ post.content | truncatewords:70 }} {% endif %}</span>
                            <span class="post-meta-snip">
                            <span style="float:right;"><a href="{{ BASE_PATH }}{{ post.url }}">Read More...</a></span> 
                            </span>
                    </div>
                </div>


                </li>
                {% endfor %}
            </ul>
        </div>
        <div class="col-sm-4">
           <div class="featured-links">
               <h3 class="content-block-header">Featured Links</h3>
               <div id="featuredLinks">
               </div>
               <script>
                   var featuredLinks = [];
                   featuredLinks.push(`
                       <br />
                       <div class="row">
                           <div class="col-md-4">
                               <a href="http://hortonworks.com/hadoop/zeppelin/" target="_blank"><img width="100px" src="{{ ASSET_PATH }}/img/hortonworks-logo.jpg" /></a>
                           </div>
                           <div class="col-md-8">
                               Zeppelin on HDP, blogs, tutorials, Presentations and more
                           </div>
                       </div>
                   `);
                   featuredLinks.push(`
                       <br />
                       <div class="row">
                           <div class="col-md-4">
                               <a href="https://aws.amazon.com/elasticmapreduce/" target="_blank"><img width="100px" src="{{ ASSET_PATH }}/img/aws-logo.jpg" /></a>
                           </div>
                           <div class="col-md-8">
                               Zeppelin and Spark on Amazon EMR, with blogs, tutorials, and more
                           </div>
                       </div>
                   `);
                   
                   function shuffle(array) {
                       var currentIndex = array.length, temporaryValue, randomIndex;

                       // While there remain elements to shuffle...
                       while (0 !== currentIndex) {

                           // Pick a remaining element...
                           randomIndex = Math.floor(Math.random() * currentIndex);
                           currentIndex -= 1;

                           // And swap it with the current element.
                           temporaryValue = array[currentIndex];
                           array[currentIndex] = array[randomIndex];
                           array[randomIndex] = temporaryValue;
                       }

                       return array;
                   }
                   featuredLinks = shuffle(featuredLinks);

                   for (i = 0; i < featuredLinks.length; i++) {
                       $("#featuredLinks").append(featuredLinks[i]);
                   }
               </script>
           </div>
           <div>
              <a class="twitter-timeline"
                  data-dnt="true"
                  href="https://twitter.com/ApacheZeppelin/lists/people"
                  data-widget-id="723760464891506688"
                  data-tweet-limit="3"
                  data-chrome="nofooter noscrollbar">

              Tweets from https://twitter.com/ApacheZeppelin/lists/people
              </a>

             <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+"://platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>
          </div>
       </div>
    </div>
</ul>



