(ns broccounting.routes.home
  (:require [compojure.core :refer :all]
            [ring.util.response :refer response]
            [broccounting.views.layout :as layout]
            [clj-http.client :as client]))

(defn home [request]
  (layout/common 
    [:div
      [:h1 "Hello World!"]
      (str request)]))

(defn tasks [session]
  (let [id (:jsessionid session)]
    (if id
      (layout/common [:h3 (str "Hello " id)])
      (layout/common 
          [:div
            [:h3 "Result"]
            [:form {:method "POST"}
             [:input {:name "login"}]
             [:input {:name "password" :type "password"}]
             [:input {:type "submit"}]]])))

(defn tasks-login [login password session]
  (let [connect-result (client/post "http://bro.myjetbrains.com/youtrack/rest/user/login"
                                    {:throw-exceptions false
                                     :form-params {:login login
                                                   :password password}})]
    (-> (response (layout/common
                    (if (= (:body connect-result) "<login>ok</login>")
                      (let [JSESSIONID (:value (get (:cookies connect-result) "JSESSIONID"))]
                        (str "JSESSIONID=" JSESSIONID))
                        (assoc session :jsessionid JSESSIONID)
                      connect-result)))
        (assoc :session session))))

(defroutes home-routes
  (GET "/" request (home request))
  (GET "/tasks" [:as {session :session}] (tasks session))
  (POST "/tasks" [login password :as {session :session}] (tasks-login login password session)))
