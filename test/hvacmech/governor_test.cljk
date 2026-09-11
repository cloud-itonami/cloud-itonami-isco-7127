(ns hvacmech.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [hvacmech.store :as store]
            [hvacmech.advisor :as advisor]
            [hvacmech.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-technician! st {:technician-id "tech-1" :name "Kobo Yamada" :certified? true})
    (store/register-service-account! st {:service-account-id "SA-1" :name "Kobo Cold Storage" :max-supply-cost 2000})
    st))

(defn- op [op-kw & {:as extra}]
  (merge {:op op-kw :effect :propose :service-account-id "SA-1"
          :confidence 0.9 :stake :low}
         extra))

(def ^:private req {:technician-id "tech-1"})

(deftest ok-log-service-record
  (let [st (fresh-store)
        v (governor/check req {} (op :log-service-record) st)]
    (is (:ok? v))))

(deftest ok-schedule-service-operation
  (let [st (fresh-store)
        v (governor/check req {} (op :schedule-service-operation) st)]
    (is (:ok? v))))

(deftest ok-supply-order-at-threshold-boundary
  (testing "the supply-cost threshold escalate boundary is exclusive (over, not at)"
    (let [st (fresh-store)
          v (governor/check req {} (op :coordinate-supply-order :cost 2000) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-technician
  (let [st (fresh-store)
        v (governor/check {:technician-id "nobody"} {} (op :log-service-record) st)]
    (is (:hard? v))
    (is (some #(= :no-technician (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-service-account
  (let [st (fresh-store)
        v (governor/check req {} (op :log-service-record :service-account-id "SA-ghost") st)]
    (is (:hard? v))
    (is (some #(= :no-service-account (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-service-record) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-outside-closed-allowlist
  (let [st (fresh-store)
        v (governor/check req {} (op :dispatch-equipment) st)]
    (is (:hard? v))
    (is (some #(= :unknown-op (:rule %)) (:violations v)))))

(deftest hard-on-scope-excluded-op-finalize-refrigerant-service-decision
  (testing "finalizing a refrigerant-system-service-execution decision is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-refrigerant-service-decision) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-authorize-refrigerant-system-service-execution
  (testing "authorizing a refrigerant-system service execution is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :authorize-refrigerant-system-service-execution) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-proceed-with-refrigerant-system-service
  (testing "proceeding with a refrigerant-system service is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :proceed-with-refrigerant-system-service) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-override-technician-certification-requirement
  (testing "overriding a technician-certification requirement is a certified technician's exclusive judgment, never this actor's"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :override-technician-certification-requirement) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-bypass-technician-certification-requirement
  (testing "bypassing a technician-certification requirement is a certified technician's exclusive judgment, never this actor's"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :bypass-technician-certification-requirement) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-even-with-allowlisted-op
  (testing "defense-in-depth: a rationale that itself attempts to proceed with the refrigerant-system service is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :schedule-service-operation)
                                           :rationale "recommend we proceed with the refrigerant-system service now")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-override-technician-certification
  (testing "defense-in-depth: a rationale attempting to override the technician-certification requirement is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-service-record)
                                           :rationale "override the technician-certification requirement and proceed")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest always-escalates-safety-concern-even-at-high-confidence
  (testing "a refrigerant-leak/pressure-anomaly/electrical-hazard concern always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :flag-safety-concern :hazard-type :refrigerant-leak)
                                           :confidence 0.99)
                             st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-supply-order-above-threshold
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :coordinate-supply-order :cost 5000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-service-record) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip-on-scope-exclusion
  (testing "the governor's scope-exclusion term list must never match the mock advisor's own default rationale text for any allowlisted op — CLAUDE.md's known self-tripping bug pattern (rationale legitimately contains bare nouns like 'refrigerant'/'certification'/'technician', but never the full finalization-action phrases)"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          ops [:log-service-record :schedule-service-operation
               :flag-safety-concern :coordinate-supply-order]]
      (doseq [o ops]
        (let [request {:technician-id "tech-1" :op o :service-account-id "SA-1"
                        :stake :low :task "routine refrigerant diagnostic task"
                        :hazard-type :refrigerant-leak :cost 500}
              proposal (advisor/-advise adv st request)
              v (governor/check request {} proposal st)]
          (is (not (:hard? v))
              (str o " proposal unexpectedly hard-blocked: " (:violations v))))))))
