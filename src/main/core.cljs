(ns main.core
  (:require [reagent.core :as reagent :refer [atom]]
            [main.data :as data]
            [hickory.core :refer [as-hiccup parse parse-fragment]]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [main.post :as post]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [ajax.core :as ajax])
  (:import goog.History))

;; grab collection from fb and set-list!
(let [fb (js/Firebase. "https://jobs-board.firebaseio.com/job-listings")]
  (.on fb "value" #(data/set-list! (js->clj (.val %)))))

;; helper, parse html into hiccup
(defn to-html [content]
  (as-hiccup (parse content)))

;; VIEWS
(defn new-post-view []
  (defn handle-input-update [event]
    (let [value     (aget event "target" "value")
          className (aget event "target" "className")]
      (data/setter className value)))

  (defn handle-contenteditable-update [event]
    (let [value     (aget event "target" "innerHTML")
          className (aget event "target" "className")]
       (data/setter className value))
    (println "blurred out of job description")
    )

  [:div
    [:input.hostel_name     {:type "text"     :placeholder "Hostel Name"     :on-change #(handle-input-update %)}]
    [:input.job_title       {:type "text"     :placeholder "Job title"       :on-change #(handle-input-update %)}]
    [:input.location        {:type "text"     :placeholder "Location"        :on-change #(handle-input-update %)}]
    [:input.email           {:type "text"     :placeholder "Email"           :on-change #(handle-input-update %)}]
    [:input.website         {:type "text"     :placeholder "website"         :on-change #(handle-input-update %)}]
    [:div.text-control

     ;document.execCommand(cmd, false, null);

     [:a {:href "#" :data-role "bold"                } "Bold"]
     [:a {:href "#" :data-role "italic"              } "Italics"]
     [:a {:href "#" :data-role "insertOrderedList"   } "Ordered List"]
     [:a {:href "#" :data-role "insertUnorderedList" } "Unordered List"]]
    [:div.job_description   {:contentEditable true
                             :placeholder "Job description"
                             :on-blur #(handle-contenteditable-update %)}]

    (let [fb (js/Firebase. "https://jobs-board.firebaseio.com/job-listings")]
      [:a {:href "#" :on-click #(data/post2fb fb)} "submit"])

    [:a.routes {:href "#/"} "home page"]
  ])

(defn job-view [uid]
  [:div#job-view "JOB POST VIEW IS HEREEE!"
    [:a.routes {:href "#/"} "Back to all jobs"]

    (if (empty? (data/get-list!))
      (println "True. Atom is empty. Do not start rendering.")
      (render-jobs-list uid))])

(defn make-date [epoch]
  (subs (.toDateString (js/Date. epoch)) 4 10))

(defn render-jobs-list [uid]
  (let [job (data/clicked-job uid)]
    [:div.job-view
     [:div.title (job "job_title")]
     [:div.date "POSTED " (make-date (job "create_date"))]
     [:div.name (job "hostel_name")]
     [:div.location (job "location")]
     [:a.website {:href (job "website")} (job "website")]



     [:div#job-description (map as-hiccup (parse-fragment (job "job_description")))]
     [:div.email (job "email")]]))

(defn home-view-item [data]
  (let [[uid hostelData] data
        target (str "/jobs/" uid)]
    [:li
      [:a {:href (str "#" target)}
        [:span.name (hostelData "hostel_name")]
        [:span.title (hostelData "job_title")]
        [:span.date (make-date (hostelData "create_date"))]]]
 ))


(defn home-view []
  [:div.home
   [:h1 "WELCOME TO THE JOBS BOARD"]

   [:a.routes {:href "#/new/job"} "POST A NEW JOB"]

   [:ul (map home-view-item (data/get-list!))]])

;; ---------------------------------------------------------------------------------------
;; ROUTING ------------------------------------------------------------------------------
(secretary/set-config! :prefix "#")

(defroute "/jobs/:uid" [uid]
  (data/clicked-job uid)
  (data/set-view! #(job-view uid)))

(defroute "/new/job" {}
  (println "setting view to /new/job")
  (data/set-view! new-post-view))

(defroute "/" {}
  (println "setting view to /..")
  (data/set-view! home-view))

(doto (History.)
  (events/listen
    EventType/NAVIGATE
    (fn [event]
      (secretary/dispatch! (.-token event))))
  (.setEnabled true))

;; END OF ROUTING ------------------------------------------------------------------------
;; ---------------------------------------------------------------------------------------


;; RENDER VIEW
(defn app-view []
  [:div.container
    [:h1.hidden {:on-click #(data/printAtom)} "show atom"]
    (@data/current-view)
   ]
 )

(reagent/render-component [app-view] (.getElementById js/document "app"))



