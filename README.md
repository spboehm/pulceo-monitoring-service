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

## Create your own MQTT broker (optional)

## Deploy with k3d

### Deploy InfluxDB for long-term data persistence

```bash
mkdir -p /tmp/pulceo-monitoring-service
mkdir -p /tmp/pulceo-monitoring-service/pms-influxdb-pv-data
mkdir -p /tmp/pulceo-monitoring-service/pms-influxdb-pv-config
```

- Deploy InfluxDB with the following command:
```bash
k3d cluster create pulceo-test --api-port 40476 --port 80:80@loadbalancer --port 8089:8089@loadbalancer --volume /tmp/pulceo-monitoring-service/pms-influxdb-pv-data:/pms/pms-influxdb-pv-data --volume /tmp/pulceo-monitoring-service/pms-influxdb-pv-config:/pms/pms-influxdb-pv-config
```

**[TODO]: Add a step to generate the secrets**

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
kubectl apply -f pms-influxdb.yaml
```

```bash
kubectl apply -f pms-deployment.yaml
```