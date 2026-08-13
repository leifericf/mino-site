(ns mino-site.drift-gate-test
  "Tests the concept behind the census drift gate: a mutated payload
  must be detectable by diffing against the committed copy."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [mino-site.parse.census :as census]))

(def payload (census/load-payload))

(deftest vendored-payload-parses
  (is (map? payload))
  (is (pos-int? (:schema-version payload))))

(deftest mutated-payload-is-detectable
  (testing "changing a coverage number produces a detectable diff"
    (let [original (pr-str payload)
          mutated  (pr-str (assoc-in payload [:coverage :headline :percent] 0.0))]
      (is (not= original mutated)
          "a mutated payload must differ from the original"))))

(deftest removed-divergence-is-detectable
  (testing "removing a divergence entry produces a detectable diff"
    (let [original (pr-str payload)
          mutated  (pr-str (update payload :divergences rest))]
      (is (not= original mutated)))))

(deftest added-signal-is-detectable
  (testing "adding a signal produces a detectable diff"
    (let [original (pr-str payload)
          mutated  (pr-str (assoc payload :signals {:fuzz {:targets []}}))]
      (is (not= original mutated)))))
