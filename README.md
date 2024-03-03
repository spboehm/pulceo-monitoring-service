# pulceo-monitoring-service

## General Prerequisites

- Make sure that the following ports are available on the local system:
  - `80/tcp`
  - `443/tcp`
  - `40476/tcp` (for k3d API server)

## Create a free MQTT broker (recommended)

- Create a basic MQTT broker on [HiveMQ](https://console.hivemq.cloud/?utm_source=HiveMQ+Pricing+Page&utm_medium=serverless+signup+CTA+Button&utm_campaign=HiveMQ+Cloud+PaaS&utm_content=serverless)
- Make sure that you select the free plan: Serverless (Free)

## Create your own MQTT broker (optional)

**TODO: Add a guide on how to create a local MQTT broker**

## Run with k3d

```bash
mkdir -p /tmp/pulceo-monitoring-service
mkdir -p /tmp/pulceo-monitoring-service/pms-influxdb-pv-data
mkdir -p /tmp/pulceo-monitoring-service/pms-influxdb-pv-config
```
```bash
k3d cluster create pulceo-test --api-port 40476 --port 80:80@loadbalancer --port 8089:8089@loadbalancer --volume /tmp/pulceo-monitoring-service/pms-influxdb-pv-data:/pms/pms-influxdb-pv-data --volume /tmp/pulceo-monitoring-service/pms-influxdb-pv-config:/pms/pms-influxdb-pv-config
```

**[TODO]: Add a step to generate the secrets**

- Apply the following kubernetes manifest to the cluster
```bash
kubectl --kubeconfig=/home/$USER/.kube/config create configmap pms-configmap \
  --from-literal=PRM_HOST=pulceo-resource-manager \
  --from-literal=INFLUXDB_URL=http://pms-influxdb:8086 \
  --from-literal=INFLUXDB_ORG=org \
  --from-literal=INFLUXDB_BUCKET=bucket
```
```bash
kubectl --kubeconfig=/home/$USER/.kube/config create secret generic pms-credentials \
  --from-literal=DOCKER_INFLUXDB_INIT_USERNAME=${DOCKER_INFLUXDB_INIT_USERNAME} \
  --from-literal=DOCKER_INFLUXDB_INIT_PASSWORD=${DOCKER_INFLUXDB_INIT_PASSWORD} \
  --from-literal=DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=${DOCKER_INFLUXDB_INIT_ADMIN_TOKEN} \
  --from-literal=INFLUXDB_TOKEN=${DOCKER_INFLUXDB_INIT_ADMIN_TOKEN} \
  --from-literal=PNA_MQTT_BROKER_URL=${PNA_MQTT_BROKER_URL} \
  --from-literal=PNA_MQTT_CLIENT_USERNAME=${PNA_MQTT_CLIENT_USERNAME} \
  --from-literal=PNA_MQTT_CLIENT_PASSWORD=${PNA_MQTT_CLIENT_PASSWORD}
```
```bash
kubectl apply -f pms-deployment.yaml
```

- Check if everything is running with: `kubectl get deployment`
```
NAME                        READY   UP-TO-DATE   AVAILABLE   AGE
pms-influxdb                1/1     1            1           35m
pulceo-monitoring-service   1/1     1            1           50s
```

- Check the exposed services with: `kubectl get svc`
```
NAME                        TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
kubernetes                  ClusterIP      10.43.0.1       <none>        443/TCP          38m
pms-influxdb-lb             LoadBalancer   10.43.40.207    172.29.0.2    8089:31408/TCP   38m
pms-influxdb                ClusterIP      10.43.44.182    <none>        8086/TCP         36m
pulceo-monitoring-service   ClusterIP      10.43.150.151   <none>        7777/TCP         3m31s
```

pulceo-monitoring-service is now running and ready to accept workloads under `http://EXTERNAL-IP`

```bash
curl -I http://localhost:80/pms/health
```
```
HTTP/1.1 200 OK
Content-Length: 2
Content-Type: text/plain;charset=UTF-8
Date: Sat, 02 Mar 2024 16:12:17 GMT
```
