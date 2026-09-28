#!/bin/bash
mkdir -p logs/benchmarks
echo "=== ETHER CLOUD BATCH SPOT JOB STARTED: $(date) ===" > logs/benchmarks/batch_spot_execution.log

# Run Resolution 4 (288,122 cells) and Resolution 5 (2,016,842 cells) on 1800 CE Industrial
for res in 4 5; do
    echo "----------------------------------------------------------" >> logs/benchmarks/batch_spot_execution.log
    echo ">>> RUNNING SCENARIO: INDUSTRIAL | RES: $res | TICKS: 24 <<<" >> logs/benchmarks/batch_spot_execution.log
    
    HEAP="10g"
    if [ "$res" -ge 5 ]; then
        HEAP="28g"
    fi
    
    java -Xms4g -Xmx$HEAP -XX:+UseG1GC --add-modules jdk.incubator.vector \
         -jar target/society-simulation-1.0.0-beta.1-executable.jar \
         --headless --mode=cluster --role=master --port=9090 \
         --secret=EtherClusterSecret2026 \
         --scenario=INDUSTRIAL --ticks=24 --cells=0 --res=$res --profile \
         >> logs/benchmarks/batch_spot_execution.log 2>&1 || true
    
    echo ">>> FINISHED SCENARIO: INDUSTRIAL | RES: $res <<<" >> logs/benchmarks/batch_spot_execution.log
done

echo "=== ETHER CLOUD BATCH SPOT JOB COMPLETED: $(date) ===" >> logs/benchmarks/batch_spot_execution.log
