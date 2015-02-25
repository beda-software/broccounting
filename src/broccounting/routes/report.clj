(ns broccounting.routes.report
  (:require [compojure.core :refer [GET POST]]
            [ring.util.response :refer [redirect response content-type]]
            [broccounting.views.layout :as layout]
            [broccounting.models.history :as history]
            [broccounting.models.report :as report]
            [broccounting.models.rate :as rate]
            [broccounting.models.elba :refer [credentials]]
            [broccounting.routes.utils :refer :all]
            [broccounting.youtrack :as youtrack]
            [broccounting.elba :as elba]))


(defn reports [session]
  (layout/common [:h2 "Reports"]
                 [:h3 "Last reports"]
                 (layout/history-list (history/history session))
                 [:form {:method "POST"}
                  [:input {:name "report_id" :type "text"}]
                  [:input {:type "submit" :value "new report"}]]))

(defn- get-full-report [report_id session]
 (let [resp (youtrack/get (str "current/reports/" report_id "/export") session)]
    (if (= 200 (:status resp))
      (let [report (report/minify (rest (:body resp)))
            report-users (report/table->users report)
            report-data (report/table->hash report)
            full-report (report/hash->table report-data (rate/rate-db session))]
        [report-users full-report])
      nil)))


(defn report [report_id session]
      (let [[report-users full-report] (get-full-report report_id session)]
        (if (nil? full-report)
          (redirect "/reports")
          (let [html-data (layout/common
                           [:h2 "Project " [:strong report_id]]
                           (layout/display-matrix full-report)
                           [:form {:method :POST}
                            [:input {:type :submit}]])
                session (history/update session report_id)
                session (rate/add-users session report-users)
                resp (content-type (response html-data)  "text/html; charset=utf-8")
                resp (assoc resp :session session)]
            resp))))


(defn create-bill [report_id session]
  (let [[_ full-report] (get-full-report report_id session)
        [login password] (credentials session)
        bill (elba/build-bill "test/1" "test contractor" full-report)
        result (elba/create-bill login password bill)]
    (layout/common result)))

(def-private-routes report-routes default-guard
  (GET  "/reports"    [:as {session :session}]    (reports session))
  (POST "/reports"    [report_id]                 (redirect (str "report/" report_id)))
  (GET  "/report/:id" [id :as {session :session}] (report id session))
  (POST "/report/:id" [id :as {session :session}] (create-bill id session)))